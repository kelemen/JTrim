General description
===================

JTrim is a collection of hopefully useful modules. JTrim modules are written in Java 7 as Gradle projects. They were generally created in NetBeans but since they are simple Gradle projects, they can be edited with any editor or IDE. For editting in NetBeans you can use my Gradle plugin which you can install from the Update Center of NetBeans (look for "Gradle Support").

For understanding the documentation of JTrim, you should read [concurrency.md](https://github.com/kelemen/JTrim/blob/master/concurrency.md).

For a more detailed guide read the [wiki pages](https://github.com/kelemen/JTrim/wiki).


JTrim Core
==========

- Dependency: none
- State of progress: 1.3.2-alpha
- Javadoc state: all the public api should be documented
- Test state: quite thorough, high coverage
- Tutorial: https://github.com/kelemen/JTrim/wiki/JTrim-Core

Description
-----------
The JTrim Core module contains generally useful classes and interfaces used by
every JTrim modules. For example: classes supporting caching, cancellation,
concurrency and event handling.

All the public api is documented in JTrim Core and it has lots of tests.


JTrim Async
===========

- Dependency: JTrim Core
- State of progress: 1.3.2-alpha
- Javadoc state: all the public api should be documented
- Test state: quite thorough, high coverage
- Tutorial: https://github.com/kelemen/JTrim/wiki/JTrim-Async

Description
-----------
The JTrim Async module provides classes and interface for asynchronous data
retrieval. This is generally useful when the thread requesting the data cannot
be blocked for some reason which is the case with the
*AWT event dispatch thread*.

All the public api is documented in JTrim Async and it has lots of tests.


JTrim GUI
=========

- Dependency: JTrim Core, JTrim Async
- State of progress: 1.3.2-alpha
- Javadoc state: all the public api should be documented
- Test state: low coverage
- Tutorial: https://github.com/kelemen/JTrim/wiki/JTrim-GUI

Description
-----------
The JTrim GUI module aims to ease the writing of complex GUIs. It intends
to help with automatically setting the enabled state of `Swing` components,
managing asynchronous tasks and components whose painting takes too much time
to be done on the *AWT event dispatch thread*.

`Swing` components in this library are likely to be adjusted without
maintaining backward compatibility.

All the public api is documented in JTrim GUI is documented but the test
coverage is low.
