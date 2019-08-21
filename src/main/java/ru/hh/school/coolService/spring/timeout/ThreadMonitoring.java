package ru.hh.school.coolService.spring.timeout;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class ThreadMonitoring {

    private final int threadCount;
    private final String poolName;
    private final ScheduledThreadPoolExecutor executor;

    public ThreadMonitoring(int executorCount) {
        this(executorCount, "ThreadMonitoring");
    }

    public ThreadMonitoring(int threadCount, String poolName) {
        this.threadCount = threadCount;
        this.poolName = poolName;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(this.poolName + "-thread-%d")
                .build();
        executor = new ScheduledThreadPoolExecutor(this.threadCount, threadFactory);
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown())
            executor.shutdownNow();
    }

    public ScheduledFuture put(Runnable task, long delay, TimeUnit timeUnit) {
        return executor.schedule(task, delay, timeUnit);
    }

}
