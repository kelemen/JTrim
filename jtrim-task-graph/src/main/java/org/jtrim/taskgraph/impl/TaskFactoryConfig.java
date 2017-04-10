package org.jtrim.taskgraph.impl;

import org.jtrim.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim.taskgraph.TaskFactoryKey;
import org.jtrim.taskgraph.TaskFactorySetup;
import org.jtrim.utils.ExceptionHelper;

public final class TaskFactoryConfig<R, I> {
    private final TaskFactoryKey<R, I> defKey;
    private final TaskFactoryGroupConfigurer configurer;
    private final TaskFactorySetup<R, I> setup;

    public TaskFactoryConfig(
            TaskFactoryKey<R, I> defKey,
            TaskFactoryGroupConfigurer configurer,
            TaskFactorySetup<R, I> setup) {

        ExceptionHelper.checkNotNullArgument(defKey, "defKey");
        ExceptionHelper.checkNotNullArgument(configurer, "configurer");
        ExceptionHelper.checkNotNullArgument(setup, "setup");

        this.defKey = defKey;
        this.configurer = configurer;
        this.setup = setup;
    }

    public TaskFactoryKey<R, I> getDefKey() {
        return defKey;
    }

    public TaskFactoryGroupConfigurer getConfigurer() {
        return configurer;
    }

    public TaskFactorySetup<R, I> getSetup() {
        return setup;
    }
}
