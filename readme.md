General description
===================

JTrim is a collection of hopefully useful modules. JTrim modules are written in Java 7 as Gradle projects. They were generally created in NetBeans but since they are simple Gradle projects, they can be edited with any editor or IDE. For editting in NetBeans you can use my Gradle plugin which you can install from the Update Center of NetBeans (look for "Gradle Support").

For understanding the documentation of JTrim, you should read [concurrency.md](concurrency.md).

For a more detailed guide read the [wiki pages](https://github.com/kelemen/JTrim/wiki),
or you might also browse the complete [API doc of all modules](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim/api/index.html).


JTrim Core
==========

- Dependency: none
- State of progress: 1.4.0
- Javadoc state: all the public api is documented
- Test state: quite thorough, high coverage

Read the [short guide](https://github.com/kelemen/JTrim/wiki/JTrim-Core) of JTrim Core or browse the
[API doc](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim-core/api/index.html) itself.

Description
-----------
The JTrim Core module contains generally useful classes and interfaces used by
every JTrim modules. For example: classes supporting caching, cancellation,
concurrency and event handling.

All the public api is documented in JTrim Core and it has lots of tests.


JTrim Async
===========

- Dependency: JTrim Core
- State of progress: 1.4.0
- Javadoc state: all the public api is documented
- Test state: quite thorough, high coverage

Read the [short guide](https://github.com/kelemen/JTrim/wiki/JTrim-Async) of JTrim Async or browse the
[API doc](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim-async/api/index.html) itself.

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
- State of progress: 1.4.0
- Javadoc state: all the public api is documented
- Test state: quite thorough, high coverage

Read the [short guide](https://github.com/kelemen/JTrim/wiki/JTrim-GUI) of JTrim GUI or browse the
[API doc](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim-gui/api/index.html) itself.

Description
-----------
The JTrim GUI module aims to ease the writing of complex GUIs. It intends
to help with automatically setting the enabled state of `Swing` components,
managing asynchronous tasks and components whose painting takes too much time
to be done on the *AWT event dispatch thread*.

All the public api is documented in JTrim GUI and it has lots of tests.
