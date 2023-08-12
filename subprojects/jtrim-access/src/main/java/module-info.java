module jtrim.access {
    exports org.jtrim2.access;

    requires transitive jtrim.executor;
    requires transitive jtrim.property;

    requires java.base;
}
