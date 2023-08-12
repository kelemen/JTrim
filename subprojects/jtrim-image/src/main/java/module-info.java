module jtrim.image {
    exports org.jtrim2.image;

    requires transitive jtrim.cache;

    requires jtrim.utils;

    requires java.base;
    requires transitive java.desktop;
}
