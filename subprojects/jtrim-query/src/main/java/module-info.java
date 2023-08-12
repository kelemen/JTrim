module jtrim.query {
    exports org.jtrim2.concurrent.query;
    exports org.jtrim2.concurrent.query.io;

    requires transitive jtrim.cache;
    requires transitive jtrim.executor;

    requires java.base;
    requires java.logging;
}
