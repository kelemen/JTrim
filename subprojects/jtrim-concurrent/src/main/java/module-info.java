module jtrim.concurrent {
    exports org.jtrim2.cancel;
    exports org.jtrim2.concurrent;
    exports org.jtrim2.concurrent.collections;
    exports org.jtrim2.event;

    requires transitive jtrim.collections;

    requires jtrim.utils;

    requires java.base;
    requires java.logging;
}
