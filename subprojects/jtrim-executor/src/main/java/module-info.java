module jtrim.executor {
    exports org.jtrim2.executor;

    requires transitive jtrim.concurrent;
    requires transitive jtrim.utils;

    requires org.slf4j;

    requires java.base;
}
