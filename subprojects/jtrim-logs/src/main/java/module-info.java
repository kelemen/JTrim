module jtrim.logs {
    exports org.jtrim2.logs;

    requires transitive jtrim.collections;

    requires jtrim.utils;

    requires java.base;
    requires transitive java.logging;
}
