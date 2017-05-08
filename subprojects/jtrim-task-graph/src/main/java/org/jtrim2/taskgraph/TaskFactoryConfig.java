package org.jtrim2.taskgraph;

import java.util.Objects;

/**
 * Defines a complete definition of a task node factory.
 *
 * <h3>Thread safety</h3>
 * The methods of this class can be safely accessed concurrently by multiple threads,
 * even without any synchronization.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are completely <I>synchronization transparent</I>.
 *
 * @param <R> the return type of the task nodes created by the defined task node factory
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   a node to be created
 *
 * @see TaskFactoryDefiner
 */
public final class TaskFactoryConfig<R, I> {
    private final TaskFactoryKey<R, I> defKey;
    private final TaskFactoryGroupConfigurer configurer;
    private final TaskFactorySetup<R, I> setup;

    /**
     * Creates a task node factory definition with the given properties.
     *
     * @param defKey the key uniquely identifying a task node factory.
     *   This argument cannot be {@code null}.
     * @param configurer the method of defining the configuration of the associated
     *   task factory. This argument cannot be {@code null}.
     * @param setup the deferred task node factory creator. This argument cannot be {@code null}.
     */
    public TaskFactoryConfig(
            TaskFactoryKey<R, I> defKey,
            TaskFactoryGroupConfigurer configurer,
            TaskFactorySetup<R, I> setup) {

        Objects.requireNonNull(defKey, "defKey");
        Objects.requireNonNull(configurer, "configurer");
        Objects.requireNonNull(setup, "setup");

        this.defKey = defKey;
        this.configurer = configurer;
        this.setup = setup;
    }

    /**
     * Returns the key uniquely identifying the associated task node factory.
     *
     * @return the key uniquely identifying the associated task node factory.
     *   This method never returns {@code null}.
     */
    public TaskFactoryKey<R, I> getDefKey() {
        return defKey;
    }

    /**
     * Returns the method of defining the configuration of the associated task factory.
     *
     * @return the method of defining the configuration of the associated task factory.
     *   This method never returns {@code null}.
     */
    public TaskFactoryGroupConfigurer getConfigurer() {
        return configurer;
    }

    /**
     * Returns the deferred task node factory creator.
     *
     * @return the deferred task node factory creator. This method never returns {@code null}.
     */
    public TaskFactorySetup<R, I> getSetup() {
        return setup;
    }
}
