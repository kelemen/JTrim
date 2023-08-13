module jtrim.task.graph {
    exports org.jtrim2.taskgraph;
    exports org.jtrim2.taskgraph.basic;

    requires transitive jtrim.executor;

    requires java.base;
    requires java.logging;
}
