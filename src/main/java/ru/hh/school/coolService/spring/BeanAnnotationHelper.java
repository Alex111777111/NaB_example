package ru.hh.school.coolService.spring;

/**
 * @author AVUmrikhin.SBT@sberbank.ru
 * 19.08.2019
 */

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * Utilities for processing {@link Bean}-annotated methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class BeanAnnotationHelper {

    public static boolean isBeanAnnotated(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, Bean.class);
    }

    public static String determineBeanNameFor(Method beanMethod) {
        // By default, the bean name is the name of the @Bean-annotated method
        String beanName = beanMethod.getName();

        // Check to see if the user has explicitly set a custom bean name...
        Bean bean = AnnotatedElementUtils.findMergedAnnotation(beanMethod, Bean.class);
        if (bean != null && bean.name().length > 0) {
            beanName = bean.name()[0];
        }

        return beanName;
    }

}