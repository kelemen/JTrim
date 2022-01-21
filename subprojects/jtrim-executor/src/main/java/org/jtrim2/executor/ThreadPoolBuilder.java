package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.TimeDuration;

/**
 * Defines a convenient build to create an executor relying on pooled threads.
 * This builder allows to select the most efficient implementation for the set
 * configuration. Therefore, it is recommended to use this builder over directly
 * creating {@link ThreadPoolTaskExecutor} or {@link SingleThreadedExecutor}. In fact,
 * this builder might select an implementation more efficient than what is otherwise
 * publicly available.
 * <P>
 * If you want to create the executor in a single expression, then consider using the
 * {@link #create(String, Consumer) create} factory method.
 *
 * <h2>Thread safety</h2>
 * Instances of this class cannot be accessed from multiple threads
 * concurrently. Concurrent access to instances of this class must be
 * externally synchronized.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see #create(String, Consumer)
 */
public final class ThreadPoolBuilder {
    private static final TimeDuration DEFAULT_IDLE_TIMEOUT = TimeDuration.seconds(5);

    private final String poolName;

    private int maxThreadCount;
    private int maxQueueSize;
    private TimeDuration idleTimeout;
    private ThreadFactory threadFactory;
    private boolean manualShutdownRequired;
    private FullQueueHandler fullQueueHandler;

    /**
     * Creates and initializes the build with the given pool name, and
     * <ul>
     *   <li>{@link #setMaxThreadCount(int) maxThreadCount} = 1</li>
     *   <li>{@link #setMaxQueueSize(int) maxQueueSize} = {@code Integer.MAX_VALUE}</li>
     *   <li>{@link #setIdleTimeout(TimeDuration) idleTimeout} = 5 seconds</li>
     *   <li>{@link #setManualShutdownRequired(boolean) manualShutdownRequired} = true</li>
     *   <li>{@link #setThreadFactory(ThreadFactory) threadFactory} = a factory creating non-daemon threads.</li>
     * </ul>
     *
     * @param poolName the name of the thread pool to be created. Unless you overwrite the
     *   thread factory with a custom implementation, this string will be part of the name
     *   of the threads of the thread pool. This argument cannot be {@code null}.
     */
    public ThreadPoolBuilder(String poolName) {
        this.poolName = Objects.requireNonNull(poolName, "poolName");
        this.maxThreadCount = 1;
        this.maxQueueSize = Integer.MAX_VALUE;
        this.idleTimeout = DEFAULT_IDLE_TIMEOUT;
        this.threadFactory = new ExecutorsEx.NamedThreadFactory(false, poolName);
        this.manualShutdownRequired = true;
        this.fullQueueHandler = FullQueueHandler.blockAlwaysHandler();
    }

    /**
     * Creates a thread pool executor using a {@code ThreadPoolBuilder}. This method is
     * useful to create the thread pool in a single expression.
     * <P>
     * This method call is effectively equivalent to
     * <pre>{@code
     * ThreadPoolBuilder builder = new ThreadPoolBuilder(poolName);
     * config.accept(builder);
     * return builder.build();
     * }</pre>
     *
     * @param poolName the name of the thread pool to be created. Unless you overwrite the
     *   thread factory with a custom implementation, this string will be part of the name
     *   of the threads of the thread pool. This argument cannot be {@code null}.
     * @param config the action called by this method to configure the
     *   {@code ThreadPoolBuilder}. This argument cannot be {@code null}.
     * @return the executor with configuration set by the configuration action. The actual
     *   implementation of the returned executor is not defined, and must not be assumed
     *   to be a specific type of executor. This method never returns {@code null}.
     */
    public static MonitorableTaskExecutorService create(String poolName, Consumer<? super ThreadPoolBuilder> config) {
        Objects.requireNonNull(config, "config");
        ThreadPoolBuilder builder = new ThreadPoolBuilder(poolName);
        config.accept(builder);
        return builder.build();
    }

    private static int positive(int value, String name) {
        return ExceptionHelper.checkArgumentInRange(value, 1, Integer.MAX_VALUE, name);
    }

    private static long nonNegative(long value, String name) {
        return ExceptionHelper.checkArgumentInRange(value, 0, Long.MAX_VALUE, name);
    }

    /**
     * Sets and overwrites previously set maximum allowed thread count. This is the
     * maximum number of threads the created executor might spawn to run tasks concurrently.
     * If you set this value to 1, then the returned executor is guaranteed to execute
     * tasks in FIFO order.
     * <P>
     * The default value for this property is 1.
     *
     * @param maxThreadCount the new maximum allowed thread count for the built executor.
     *   This value must be greater than or equal to 1.
     */
    public void setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = positive(maxThreadCount, "maxThreadCount");
    }

    /**
     * Sets and overwrites previously set maximum queue size for the queue holding tasks
     * to be executed once there is an available thread.
     * <P>
     * The default value for this property is {@code Integer.MAX_VALUE}.
     *
     * @param maxQueueSize the new maximum allowed thread count for the built executor.
     *   This value must be greater than or equal to 1.
     */
    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = positive(maxQueueSize, "maxQueueSize");
    }

    /**
     * Sets and overwrites previously set timeout value for threads to expire after they
     * had nothing to do for the given amount of time. When needed, new threads will be
     * created in place of the expired threads up to {@link #setMaxThreadCount(int) maximum allowed thread count}.
     * <P>
     * The default value for this property is 5 seconds.
     *
     * @param idleTimeout the timeout value for idle threads to expire. This value can
     *   be {@code null} to set infinite timeout (i.e. threads will never expire unless
     *   the executor shuts down). The timeout may not be negative (but can be zero).
     */
    public void setIdleTimeout(TimeDuration idleTimeout) {
        if (idleTimeout != null) {
            nonNegative(idleTimeout.getDuration(idleTimeout.getNativeTimeUnit()), "timeout");
        }
        this.idleTimeout = idleTimeout;
    }

    /**
     * Sets and overwrites previously set factory to create new threads when the executor
     * needs them. The passed configuration can be used to set whatever property is currently
     * supported to be customized. If you need more customization, then consider setting
     * your own implementation of {@link ThreadFactory}.
     * <P>
     * The default value for this property is a thread factory creating non-daemon threads
     * naming the threads based on the pool name.
     *
     * @param config the action configured the property for threads to be created. This
     *   configuration is called immediately by this method, and captured values of the
     *   thread factory will remain the same after. This argument cannot be {@code null}.
     */
    public void setThreadFactoryWithConfig(Consumer<? super ThreadFactoryConfig> config) {
        Objects.requireNonNull(config, "config");

        ThreadFactoryConfig builder = new ThreadFactoryConfig(poolName);
        config.accept(builder);
        setThreadFactory(builder.build());
    }

    /**
     * Sets and overwrites previously set factory to create new threads when the executor
     * needs them. Note that you can create a completely custom name for the started
     * threads, and the executor won't rename them. This also means, that in this case
     * the pool name has little meaning.
     * <P>
     * The default value for this property is a thread factory creating non-daemon threads
     * naming the threads based on the pool name.
     *
     * @param threadFactory the factory to create new threads when the executor
     *   needs them. This argument cannot be {@code null}.
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
    }

    /**
     * Sets and overwrites previously set value marking the executor to require explicit
     * shutdown or not. The executor built is not required to make use of this property,
     * but it might implement some error detection in case the executor was not shutdown
     * explicitly. However, if you are creating an executor with 0 timeout, then shutting
     * down the executor is not necessary to avoid having unnecessary threads, in which case
     * you might never shut down the executor, in which case you must set this property to
     * {@code false}.
     * <P>
     * The default value for this property is {@code true}.
     *
     * @param manualShutdownRequired {@code true} if the built executor requires explicit shutdown,
     *   {@code false} otherwise
     */
    public void setManualShutdownRequired(boolean manualShutdownRequired) {
        this.manualShutdownRequired = manualShutdownRequired;
    }

    /**
     * Sets and overwrites previously set handler defining a custom exception to be thrown
     * in case the task queue of the executor is full.
     * <P>
     * The default value for this property is a handler always instructing the executor
     * to block and wait until it can execute the task.
     *
     * @param fullQueueHandler the new handler defining a custom exception to be thrown
     *   in case the task queue of the executor is full. This argument cannot be {@code null}.
     */
    public void setFullQueueHandler(FullQueueHandler fullQueueHandler) {
        this.fullQueueHandler = Objects.requireNonNull(fullQueueHandler, "fullQueueHandler");
    }

    private FullQueueHandler getOptimizedFullQueueHandler() {
        FullQueueHandler result = fullQueueHandler;
        return result == FullQueueHandler.blockAlwaysHandler()
                ? null
                : result;
    }

    private boolean isInfiniteTimeout() {
        // Nobody will really notice, because it is 200+ years even in nanos.
        // The main benefit is that it is not unusual for a code to pass the maximum
        // timeout for infinity.
        TimeDuration currentTimeout = idleTimeout;
        return currentTimeout == null
                || currentTimeout.getDuration(currentTimeout.getNativeTimeUnit()) == Long.MAX_VALUE;
    }

    private TimeDuration getSafeIdleTimeout() {
        // We will never call this for now, because we have special implementation when lacking timeout.
        return Objects.requireNonNull(idleTimeout, "idleTimeout");
    }

    private SimpleThreadPoolTaskExecutor buildNoTimeoutExecutor() {
        SimpleThreadPoolTaskExecutor result
                = new SimpleThreadPoolTaskExecutor(poolName, maxThreadCount, maxQueueSize, threadFactory);

        result.setFullQueueHandler(getOptimizedFullQueueHandler());
        if (!manualShutdownRequired) {
            result.dontNeedShutdown();
        }
        return result;
    }

    private SingleThreadedExecutor buildSingleThreadedExecutor() {
        SingleThreadedExecutor result = new SingleThreadedExecutor(
                poolName,
                maxQueueSize,
                getSafeIdleTimeout(),
                threadFactory
        );
        result.setFullQueueHandler(getOptimizedFullQueueHandler());
        if (!manualShutdownRequired) {
            result.dontNeedShutdown();
        }
        return result;
    }

    private ThreadPoolTaskExecutor buildGenericExecutor() {
        ThreadPoolTaskExecutor result = new ThreadPoolTaskExecutor(
                poolName,
                maxThreadCount,
                maxQueueSize,
                getSafeIdleTimeout(),
                threadFactory
        );
        result.setFullQueueHandler(getOptimizedFullQueueHandler());
        if (!manualShutdownRequired) {
            result.dontNeedShutdown();
        }
        return result;
    }

    /**
     * Creates and returns a new executor using the previously set properties. This method
     * captures the previously set properties, and subsequent modifications to this builder
     * has no effect on the returned executor.
     * <P>
     * Note: This method makes no guarantee on the type of the returned executor, and the
     * actual implementation might change in future versions.
     *
     * @return a new executor using the previously set properties. This method never returns
     *   {@code null}.
     */
    public MonitorableTaskExecutorService build() {
        if (isInfiniteTimeout()) {
            return buildNoTimeoutExecutor();
        }
        if (maxThreadCount == 1) {
            return buildSingleThreadedExecutor();
        }
        return buildGenericExecutor();
    }

    /**
     * Defines a configurer to create a simple {@link ThreadFactory} instance.
     *
     * @see ThreadPoolBuilder#setThreadFactoryWithConfig(Consumer) ThreadPoolBuilder.setThreadFactoryWithConfig
     */
    public static final class ThreadFactoryConfig {
        private final String poolName;
        private boolean daemon;

        private ThreadFactoryConfig(String poolName) {
            this.poolName = poolName;
            this.daemon = false;
        }

        /**
         * Sets and overwrites previously set value to mark new threads daemon or not.
         * <P>
         * The default value for this property is {@code false}.
         *
         * @param daemon {@code true} if the newly created threads must be daemon threads,
         *   {@code false} otherwise
         */
        public void setDaemon(boolean daemon) {
            this.daemon = daemon;
        }

        private ThreadFactory build() {
            return new ExecutorsEx.NamedThreadFactory(daemon, poolName);
        }
    }
}
