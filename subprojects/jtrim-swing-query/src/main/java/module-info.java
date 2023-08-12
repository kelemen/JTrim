module jtrim.swing.query {
    exports org.jtrim2.swing.concurrent.query;

    requires transitive jtrim.ui.query;
    requires transitive jtrim.swing.concurrent;

    requires java.base;
}
