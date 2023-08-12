module jtrim.executor {
    exports org.jtrim2.executor;

    requires transitive jtrim.concurrent;
    requires transitive jtrim.utils;

    requires java.base;
    requires java.logging;
}
