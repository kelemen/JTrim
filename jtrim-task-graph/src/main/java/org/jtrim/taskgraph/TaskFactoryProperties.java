package org.jtrim.taskgraph;

public class TaskFactoryProperties {
    private final TaskNodeProperties defaultNodeProperties;

    private TaskFactoryProperties(Builder builder) {
        this.defaultNodeProperties = builder.defaultNodeProperties.build();
    }

    public TaskNodeProperties getDefaultNodeProperties() {
        return defaultNodeProperties;
    }

    public static class Builder {
        private final TaskNodeProperties.Builder defaultNodeProperties;

        public Builder() {
            this.defaultNodeProperties = new TaskNodeProperties.Builder();
        }

        public Builder(TaskFactoryProperties defaults) {
            this.defaultNodeProperties = new TaskNodeProperties.Builder(defaults.getDefaultNodeProperties());
        }

        public TaskNodeProperties.Builder defaultNodeProperties() {
            return defaultNodeProperties;
        }

        public TaskFactoryProperties build() {
            return new TaskFactoryProperties(this);
        }
    }
}
