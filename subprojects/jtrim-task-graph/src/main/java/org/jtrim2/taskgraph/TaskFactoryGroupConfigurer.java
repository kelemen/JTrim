package org.jtrim2.taskgraph;

/**
 * Defines the configuration of a task node factory. Implementations
 * of this interface must be repeatable without any side effect.
 *
 * <h3>Thread safety</h3>
 * The method of this interface must be safely callable concurrently
 * from multiple threads.
 *
 * <h4>Synchronization transparency</h4>
 * The method of this interface is not required to be <I>synchronization transparent</I>.
 * However, the method of this interface must expected to be called from any thread.
 *
 * @see TaskGraphDefConfigurer
 */
public interface TaskFactoryGroupConfigurer {
    /**
     * Configures the associated task node factory.
     *
     * @param properties the properties of the associated task node factory to
     *   be configured. This argument may only be used until this method returns.
     *   This argument cannot be {@code null}.
     */
    public void setup(TaskFactoryProperties.Builder properties);
}
