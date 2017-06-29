Event Tracking
==============

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-event-track:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-event-track</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-executor"
  - "org.jtrim2:jtrim-concurrent"
    - "org.jtrim2:jtrim-collections"
      - "org.jtrim2:jtrim-utils"


Description
-----------

This module allows tracking what action caused what. The causes are tracked
by an implementation of the `EventTracker` interface. The `EventTracker`
interface allows creating listener managers and they can track event causes
the following ways:

- If a listener notified by a listener manager of the `EventTracker` causes an
  event to be fired for another listener manager also created by the same
  `EventTracker`. The second event notification is said to be caused by the
  first (calling) event notification.
- An `EventTracker` allows wrapping executors (`TaskExecutor`). If an event
  listener submits a task to such an executor (possibly indirectly), the
  submitted task is treated as if it was called synchronously for event cause
  tracking purposes.

This event tracking mechanism was designed to detect circular event
notifications. Circular event notifications naturally happen when two components
are kept in sync with each other regardless which is changed by the user. For
example: When showing an image (possibly zoomed), one might display a small
overview of the whole image. This small overview can display which part of the
image is shown and might also allow the user to select which part of the image
is to be shown in the main window. Since the image overview and the main image
display must be kept sync, each of them must update the other one when the image
is panned. This module allows one generic solution to such a problem to prevent
the recursion by avoiding recursive event notification.


### Core interface ###

- `EventTracker`: Allows tracking causes of events.

### Core classes ###

- `LinkedEventTracker`: A generic implementation of `EventTracker`.
- `LocalEventTracker`: An implementation of `EventTracker` allowing to remove
  all registered listeners in a single method call.
