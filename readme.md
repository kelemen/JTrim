General description
===================

JTrim is a collection of several hopefully useful modules. JTrim modules are
written in Java as [Maven](http://maven.apache.org) projects. They were
generally created in NetBeans but since they are simple Maven projects, they
can be edited with any editor or IDE. Note that JTrim modules use Java 7.

For understanding the documentation of JTrim, you should read [concurrency.md](https://github.com/kelemen/JTrim/blob/master/concurrency.md).


JTrim Core
==========

- Dependency: none
- State of progress: 1.0-alpha
- Javadoc state: near complete

Description
-----------
The JTrim Core module contains generally useful classes and interfaces used by
every JTrim modules. For example: classes supporting caching, concurrency
and event handling.

All the public api is documented in JTrim Core. It still lacks unit tests
however.


JTrim Async
===========

- Dependency: JTrim Core
- State of progress: 1.0-alpha
- Javadoc state: near complete

Description
-----------
The JTrim Async module provides classes and interface for asynchronous data
retrieval. This is generally useful when the thread requesting the data cannot
be blocked for some reason which is the case with the
*AWT event dispatch thread*.

There are future plans to add additional implementations ease the writing
of asynchronous (cancelable) stream reading.

All the public api is documented in JTrim Async. It still lacks unit tests
however.


JTrim GUI
=========

- Dependency: JTrim Core, JTrim Async
- State of progress: 0.6 (pre-alpha)
- Javadoc state: lacking

Description
-----------
The JTrim Async module aims to ease the writing of complex GUIs. It intends
to help with automatically setting the enabled state of `Swing` components,
managing asynchronous tasks and components whose painting takes too much time
to be done on the *AWT event dispatch thread*.

Classes and interfaces in this library are likely to be refactored without
maintaining backward compatibility. Also implementations are likely to be
rewritten and new ones will be introduced.

This module has little to non documentation and also lacks unit tests.
