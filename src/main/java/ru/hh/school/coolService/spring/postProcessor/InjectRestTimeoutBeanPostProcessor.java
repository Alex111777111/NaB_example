package ru.hh.school.coolService.spring.postProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.asm.Type;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.cglib.transform.ClassEmitterTransformer;
import org.springframework.cglib.transform.TransformingClassGenerator;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import ru.hh.school.coolService.spring.MethodTimeout;
import ru.hh.school.coolService.spring.RestTimeout;
import ru.hh.school.coolService.spring.timeout.ThreadMonitoring;

import javax.ws.rs.Path;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InjectRestTimeoutBeanPostProcessor implements BeanPostProcessor {


    private static final Logger logger = LoggerFactory.getLogger(InjectRestTimeoutBeanPostProcessor.class);

    private Map<String, Class> endpointClasses = new HashMap<>();

    private static final String BEAN_FACTORY_FIELD = "$$beanFactory";


    private static final SpringObjenesis objenesis = new SpringObjenesis();


    /**
     * <bean, <method, timeout>>
     */
    private Map<String, Map<String, MethodTimeout>> endpointTimeouts = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        logger.info("===> postProcessBeforeInitialization(): " + beanName);
        final Class<?> beanClass = bean.getClass();
        for (Annotation annotation : beanClass.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(Path.class)) {
                endpointClasses.put(beanName, beanClass);
            }
        }
        return bean;
    }

    /**
     * Create proxy over EndPoint (@Path) with timeout from SUP on Class/method
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        logger.info("===> postProcessAfterInitialization(): " + beanName);

        final Class endpointClass = endpointClasses.get(beanName);
        if (endpointClass == null) {
            return bean;
        }
        RestTimeout classTimeout = getRestTimeout(endpointClass.getDeclaredAnnotations());

        Map<String, MethodTimeout> methodTimeouts = new HashMap<>();
        for (Method method : endpointClass.getDeclaredMethods()) {
            RestTimeout methodTimeout = getRestTimeout(method.getDeclaredAnnotations());
            if (methodTimeout != null) {
                methodTimeouts.put(method.getName(), getTimeout(methodTimeout));
            } else if (classTimeout != null) {
                methodTimeouts.put(method.getName(), getTimeout(classTimeout));
            } else {
                logger.error("postProcessAfterInitialization(): no timeouts bean - " + beanName);
            }
        }
        endpointTimeouts.put(beanName, methodTimeouts);

/*
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(RestTimeout.class)) {
                        // Запустить таймер который задан.
                        methodTimeout = (RestTimeout) annotation;
                    }
                }
*/

        Object aopProxy = getAopProxy(bean);

        MethodInterceptor callback = new MethodInterceptor() {


            private ThreadMonitoring threadMonitor = new ThreadMonitoring(3, "HttpTimeoutPool");

            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                logger.info("===> intercept(): " + beanName);
                final Map<String, MethodTimeout> methodTimeouts = endpointTimeouts.get(beanName);
                if (methodTimeouts != null) {
                    MethodTimeout timeout = methodTimeouts.get(method.getName());



                    Object object = null;
                    if (objects.length > 0) {
                        object = objects[0];
                    }
                    logger.info("bean: " + beanName + ", param: " + object + ", method: " + method.getName() + ", timeout: " + timeout);

                    initTimeoutService(timeout);
                }
                return methodProxy.invoke(bean, objects);
            }

            private void initTimeoutService(MethodTimeout methodTimeout) {
                long timeout = getTimeout(methodTimeout);

                if (timeout > 0) {
                    final Thread parentThread = Thread.currentThread();
                    threadMonitor.put(new Runnable() {
                        @Override
                        public void run() {
                            if (parentThread.isAlive() && !parentThread.isInterrupted()) {
                                logger.warn("Request processing time exceeded {} ms. Interrupting thread {}", timeout, parentThread.getName());
                                //TODO send Errors Timeout Response

                                parentThread.interrupt();
                            }
                        }
                    }, timeout, TimeUnit.MILLISECONDS);
                }

/*
                final HttpServletRequest httpRequest = ServletUtil.getHttpRequest();
                final HttpServletResponse httpResponse = ServletUtil.getHttpResponse();
                if (httpRequest != null) {
                    UfsHttpRequestWrapper request = new UfsHttpRequestWrapper(httpRequest, httpResponse, null, null);
                    UfsHttpResponseWrapper response = new UfsHttpResponseWrapper(request, httpResponse);
                    timeoutService.installTimeoutListener(request, response);
                }
*/
            }

            private long getTimeout(MethodTimeout methodTimeout) {
                long timeout = -1;
                try {
                    timeout = Long.parseLong(methodTimeout.getValue());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                return timeout;
            }


        };

        ClassLoader classloader = ClassUtils.getDefaultClassLoader();
/*
        Class<?> enhanceClass = enhance(bean.getClass(), classloader);
*/
        try {

/*
            Enhancer enhancer = new Enhancer();
            enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classloader));
            enhancer.setSuperclass(enhanceClass);
            enhancer.setCallback(callback);

            Object proxy = enhancer.create();
*/

            Object proxy = createCglibProxyForFactoryBean(bean, callback);

/*
            Object proxy = Enhancer.create(enhanceClass, callback);
*/

/*
            Object proxy = Enhancer.create(bean.getClass(), callback);
*/
            return proxy;
        } catch (Exception e) {
            logger.error("ERROR!: " + beanName + ", " + bean.getClass() + ", " + e.getMessage(), e);
            return bean;
        }
    }


    private MethodTimeout getTimeout(RestTimeout timeoutAnnotation) {
        MethodTimeout timeout = new MethodTimeout();
        timeout.setTimeoutSupName(timeoutAnnotation.key());
        timeout.setValue(timeoutAnnotation.value());
        return timeout;
    }

    private Object getAopProxy(Object bean) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(bean);
        return proxyFactory.getProxy();
    }

    private RestTimeout getRestTimeout(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(RestTimeout.class)) {
                return (RestTimeout) annotation;
            }
        }
        return null;
    }


    // Создание прокси, используя параметризованный конструктор

    private Object createCglibProxyForFactoryBean(Object factoryBean, MethodInterceptor callback) {
/*
    private Object createCglibProxyForFactoryBean(final Object factoryBean,
                                                  final ConfigurableBeanFactory beanFactory, final String beanName) {
*/

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(factoryBean.getClass());
        enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
        enhancer.setCallbackType(MethodInterceptor.class);

        // Ideally create enhanced FactoryBean proxy without constructor side effects,
        // analogous to AOP proxy creation in ObjenesisCglibAopProxy...
        Class<?> fbClass = enhancer.createClass();
        Object fbProxy = null;

        if (objenesis.isWorthTrying()) {
            try {
                fbProxy = objenesis.newInstance(fbClass, enhancer.getUseCache());
            } catch (ObjenesisException ex) {
                logger.debug("Unable to instantiate enhanced FactoryBean using Objenesis, " +
                        "falling back to regular construction", ex);
            }
        }

        if (fbProxy == null) {
            try {
                fbProxy = ReflectionUtils.accessibleConstructor(fbClass).newInstance();
            } catch (Throwable ex) {
                throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using Objenesis, " +
                        "and regular FactoryBean instantiation via default constructor fails as well", ex);
            }
        }

/*
        ((Factory) fbProxy).setCallback(0, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                if (method.getName().equals("getObject") && args.length == 0) {
                    return beanFactory.getBean(beanName);
                }
                return proxy.invoke(factoryBean, args);
            }
        });
*/
        ((Factory) fbProxy).setCallback(0, callback);

        return fbProxy;
    }

    /**
     * Conditional {@link Callback}.
     *
     * @see ConditionalCallbackFilter
     */
    private interface ConditionalCallback extends Callback {

        boolean isMatch(Method candidateMethod);
    }


    /**
     * A {@link CallbackFilter} that works by interrogating {@link Callback}s in the order
     * that they are defined via {@link ConditionalCallback}.
     */
    private static class ConditionalCallbackFilter implements CallbackFilter {

        private final Callback[] callbacks;

        private final Class<?>[] callbackTypes;

        public ConditionalCallbackFilter(Callback[] callbacks) {
            this.callbacks = callbacks;
            this.callbackTypes = new Class<?>[callbacks.length];
            for (int i = 0; i < callbacks.length; i++) {
                this.callbackTypes[i] = callbacks[i].getClass();
            }
        }

        @Override
        public int accept(Method method) {
            for (int i = 0; i < this.callbacks.length; i++) {
                if (!(this.callbacks[i] instanceof ConditionalCallback) ||
                        ((ConditionalCallback) this.callbacks[i]).isMatch(method)) {
                    return i;
                }
            }
            throw new IllegalStateException("No callback available for method " + method.getName());
        }

        public Class<?>[] getCallbackTypes() {
            return this.callbackTypes;
        }
    }


    /**
     * Custom extension of CGLIB's DefaultGeneratorStrategy, introducing a {@link BeanFactory} field.
     * Also exposes the application ClassLoader as thread context ClassLoader for the time of
     * class generation (in order for ASM to pick it up when doing common superclass resolution).
     */
    private static class BeanFactoryAwareGeneratorStrategy extends DefaultGeneratorStrategy {

        @Nullable
        private final ClassLoader classLoader;

        public BeanFactoryAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        protected ClassGenerator transform(ClassGenerator cg) throws Exception {
            ClassEmitterTransformer transformer = new ClassEmitterTransformer() {
                @Override
                public void end_class() {
                    declare_field(Constants.ACC_PUBLIC, BEAN_FACTORY_FIELD, Type.getType(BeanFactory.class), null);
                    super.end_class();
                }
            };
            return new TransformingClassGenerator(cg, transformer);
        }

        @Override
        public byte[] generate(ClassGenerator cg) throws Exception {
            if (this.classLoader == null) {
                return super.generate(cg);
            }

            Thread currentThread = Thread.currentThread();
            ClassLoader threadContextClassLoader;
            try {
                threadContextClassLoader = currentThread.getContextClassLoader();
            } catch (Throwable ex) {
                // Cannot access thread context ClassLoader - falling back...
                return super.generate(cg);
            }

            boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
            if (overrideClassLoader) {
                currentThread.setContextClassLoader(this.classLoader);
            }
            try {
                return super.generate(cg);
            } finally {
                if (overrideClassLoader) {
                    // Reset original thread context ClassLoader.
                    currentThread.setContextClassLoader(threadContextClassLoader);
                }
            }
        }
    }

}
