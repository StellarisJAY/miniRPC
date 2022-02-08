package com.jay.rpc.util;

import com.jay.dove.util.NamedThreadFactory;

import java.util.concurrent.*;

/**
 * <p>
 *  线程池工具
 *  请使用该工具提供的对应场景的线程池
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 11:28
 */
public class ThreadPoolUtil {

    private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(2,
            new NamedThreadFactory("scheduler-"));

    /**
     * 获取一个线程池
     * 默认使用CallerRun拒绝策略
     * @param coreSize coreSize
     * @param maxSize maxSize
     * @param keepAliveTime keepAliveTime
     * @param timeUnit TimeUnit
     * @param threadName threadName prefix
     * @return {@link ExecutorService}
     */
    public static ExecutorService newThreadPool(int coreSize, int maxSize, long keepAliveTime, TimeUnit timeUnit,  String threadName){
        return new ThreadPoolExecutor(coreSize, maxSize, keepAliveTime, timeUnit,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(threadName),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 获取一个IO密集型任务线程池
     * @param threadName 线程名 prefix
     * @return {@link ExecutorService}
     */
    public static ExecutorService newIoThreadPool(String threadName){
        return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(threadName),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static void schedule(Runnable task, long delay, TimeUnit timeUnit){
        SCHEDULER.schedule(task, delay, timeUnit);
    }

    public static void scheduleAtFixedRate(Runnable task, long delay, long period, TimeUnit timeUnit){
        SCHEDULER.scheduleAtFixedRate(task, delay, period, timeUnit);
    }
}
