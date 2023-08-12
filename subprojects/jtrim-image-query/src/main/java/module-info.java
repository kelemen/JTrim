module jtrim.image.query {
    exports org.jtrim2.image.async;

    requires transitive jtrim.image;
    requires transitive jtrim.query;

    requires jtrim.utils;

    requires java.base;
}
