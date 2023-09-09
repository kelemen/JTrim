module jtrim.query {
    exports org.jtrim2.concurrent.query;
    exports org.jtrim2.concurrent.query.io;

    requires transitive jtrim.cache;
    requires transitive jtrim.executor;

    requires org.slf4j;

    requires java.base;
}
