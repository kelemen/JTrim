module jtrim.ui.query {
    exports org.jtrim2.ui.concurrent.query;

    requires transitive jtrim.query;
    requires transitive jtrim.ui.concurrent;

    requires org.slf4j;

    requires java.base;
}
