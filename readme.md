General description
-------------------

JTrim is a collection of multiple modules with various purpose. Including the
following:

- Cancellation support (similar to .NET's)
- Executor framework with better cancellation support and native support
  for Java 8's `CompletionStage`. Also, there are many kinds of executor
  implementations.
- Generic event handling utilities.
- Generic property handling (properties with change listeners).
- Support for automatic UI state management.
- Better support for background task processing in UI applications.
- Asynchronous data retrieval (mostly aiming UIs).


For understanding the documentation of JTrim, you should read [concurrency.md](concurrency.md).
You may also want to browse the [API doc of all modules](http://htmlpreview.github.com/?https://github.com/kelemen/api-docs/blob/jtrim/api/index.html).

All released modules have a very high test coverage and all public API is
documented (though there are typos to be fixed).


Using JTrim
-----------

JTrim binaries are hosted in [Central](https://repo1.maven.org/maven2): https://repo1.maven.org/maven2.

### Example usage in Gradle

    repositories {
        mavenCentral()
    }
    dependencies {
        compile "org.jtrim2:jtrim-executor:${jtrimVersion}"
    }

### Example usage in Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-executor</artifactId>
            <scope>compile</scope>
        </dependency>
    </dependencies>


List of modules
---------------

All modules have the group id: "org.jtrim2"

- [jtrim-access](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-access/readme.md):
  Manages resources between cooperating code. This module was designed to
  provide support for UIs to manage what tasks are allowed to run and what not.
- [jtrim-cache](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-cache/readme.md):
  Provides basic caching support. The caching was designed for the *jtrim-query*
  module and is not a generic caching library. It is best thought of as the
  generalization of weak and soft references.
- [jtrim-collections](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-collections/readme.md):
  Adds various collection utilities supplementing the JDK.
- [jtrim-concurrent](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-concurrent/readme.md):
  Provides cancellation support, generic event handling and some basic
  concurrency utilities.
- [jtrim-event-track](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-event-track/readme.md):
  Allows event handling where it is possible to track the causes of events.
- [jtrim-executor](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-executor/readme.md):
  A new executor framework with better cancellation, and native support for
  Java 8's `CompletionStage`.
- [jtrim-image](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-image/readme.md):
  Provides small utilities for working with `BufferedImage`.
- [jtrim-image-query](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-image-query/readme.md):
  This module provides support to load images asynchronously using ImageIO.
- [jtrim-image-transform](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-image-transform/readme.md):
  Provides common image transformation for `BufferedImage`. The transformation
  assumes that there is a fixed size destination (like a UI component).
- [jtrim-logs](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-logs/readme.md):
  Provides small utilities for *java.util.logging*, mostly helping testing
  logging.
- [jtrim-property](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-property/readme.md):
  Provides generic property handling utilities (properties with change listeners).
- [jtrim-query](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-query/readme.md):
  Provides asynchronous query support.
- [jtrim-stream](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-stream/readme.md):
  Provides conveniently implementable yet powerful stream support (instead of Java's Stream API).
- [jtrim-swing-component](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-swing-component/readme.md):
  Provides *Swing* components with support for rendering in the background.
- [jtrim-swing-concurrent](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-swing-concurrent/readme.md):
  Provides support for background task execution for *Swing* applications. This
  module simply extends the support provided by the *jtrim-ui-concurrent* module.
- [jtrim-swing-property](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-swing-property/readme.md):
  Provides utility to map *Swing* properties to JTrim based properties and to
  bind properties to prevent unwanted UI access.
- [jtrim-swing-query](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-swing-query/readme.md):
  Provides support for asynchronous queries for *Swing* applications. This
  module simply extends the support provided by the *jtrim-ui-property* module.
- [jtrim-task-graph](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-task-graph/readme.md):
  Allows executing a computation graph with various resource constraints.
- [jtrim-ui-concurrent](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-ui-concurrent/readme.md):
  Provides support for background task execution for UI applications. This module
  builds on the support provided by the *jtrim-access* module to allow you to
  prevent access to UI or the prevent starting the background task.
- [jtrim-ui-query](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-ui-query/readme.md):
  Provides support for asynchronous queries for UI applications. This module
  builds on the support provided by the *jtrim-access* module to allow you to
  prevent access to UI or the prevent starting the asynchronous query.
- [jtrim-utils](https://github.com/kelemen/JTrim/blob/master/subprojects/jtrim-utils/readme.md):
  This module provides some very basic utility used by all JTrim modules.
