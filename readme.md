General description
===================

JTrim is a collection of hopefully useful modules. JTrim modules are written in Java 7 as Gradle projects. They were generally created in NetBeans but since they are simple Gradle projects, they can be edited with any editor or IDE. For editting in NetBeans you can use my Gradle plugin which you can install from the Update Center of NetBeans (look for "Gradle Support").

For understanding the documentation of JTrim, you should read [concurrency.md](concurrency.md).

For a more detailed guide read the [wiki pages](https://github.com/kelemen/JTrim/wiki),
or you might also browse the complete [API doc of all modules](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim/api/index.html).

All released modules have a very high test coverage and all public API is documented (though there are typos to be fixed).

**Latest release: 1.7.1**

Using JTrim
===========

JTrim binaries are hosted in a Maven repository at [Bintray](https://bintray.com): http://dl.bintray.com/kelemen/maven.

Example usage in Gradle
-----------------------

    repositories {
        maven {
            url 'http://dl.bintray.com/kelemen/maven'
        }
    }
    dependencies {
        compile "org.jtrim:jtrim-gui:1.7.1"
    }

Example usage in Maven
----------------------

    <repositories>
        <repository>
            <id>jtrim-releases</id>
            <url>http://dl.bintray.com/kelemen/maven</url>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>org.jtrim</groupId>
            <artifactId>jtrim-gui</artifactId>
            <version>1.7.1</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>

JTrim Core
==========

- GroupId: org.jtrim
- ArtifactId: jtrim-core
- Dependency: none

Read the [short guide](https://github.com/kelemen/JTrim/wiki/JTrim-Core) of JTrim Core or browse the
[API doc](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim-core/api/index.html) itself.

Description
-----------
The JTrim Core module contains generally useful classes and interfaces used by
every JTrim modules. For example: classes supporting caching, cancellation,
concurrency and event handling.


JTrim Async
===========

- GroupId: org.jtrim
- ArtifactId: jtrim-async
- Dependency: JTrim Core (exported)

Read the [short guide](https://github.com/kelemen/JTrim/wiki/JTrim-Async) of JTrim Async or browse the
[API doc](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim-async/api/index.html) itself.

Description
-----------
The JTrim Async module provides classes and interface for asynchronous data
retrieval. This is generally useful when the thread requesting the data cannot
be blocked for some reason which is the case with the
*AWT event dispatch thread*.


JTrim GUI
=========

- GroupId: org.jtrim
- ArtifactId: jtrim-gui
- Dependency: JTrim Core (exported), JTrim Async (exported)

Read the [short guide](https://github.com/kelemen/JTrim/wiki/JTrim-GUI) of JTrim GUI or browse the
[API doc](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim-gui/api/index.html) itself.

Description
-----------
The JTrim GUI module aims to ease the writing of complex GUIs. It intends
to help with automatically setting the enabled state of `Swing` components,
managing asynchronous tasks and components whose painting takes too much time
to be done on the *AWT event dispatch thread*.
