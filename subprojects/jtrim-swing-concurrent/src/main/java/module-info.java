module jtrim.swing.concurrent {
    exports org.jtrim2.swing.concurrent;

    requires transitive jtrim.ui.concurrent;

    requires java.base;
    requires transitive java.desktop;
}
