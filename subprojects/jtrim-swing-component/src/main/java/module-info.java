module jtrim.swing.component {
    exports org.jtrim2.swing.component;

    requires transitive jtrim.access;
    requires transitive jtrim.collections;
    requires transitive jtrim.concurrent;
    requires transitive jtrim.executor;
    requires transitive jtrim.image.query;
    requires transitive jtrim.image.transform;
    requires transitive jtrim.property;
    requires transitive jtrim.ui.concurrent;
    requires transitive jtrim.ui.query;
    requires transitive jtrim.utils;
    requires transitive jtrim.swing.concurrent;
    requires transitive jtrim.swing.property;
    requires transitive jtrim.swing.query;

    requires java.base;
    requires transitive java.desktop;
    requires java.logging;
}
