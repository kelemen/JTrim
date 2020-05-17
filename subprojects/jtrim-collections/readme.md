New Collections
===============

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-collections:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-collections</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-utils"


Description
-----------

This module provides additional classes and interfaces to work with
collections. The two main interfaces here are `RefCollection` and `RefList`.
They allow you to keep a reference to elements added to a collection and use
this reference to adjust the position of this element in a list or remove it
from a collection. These interfaces were created mainly to allow exploitation of
all the benefits of a linked list (e.g.: remove an element which you have added
in constant time). Note however, that a new implementation of `PriorityQueue`
could also benefit from the `RefCollection` interface.

### Examples

It is well known that `java.util.LinkedList` is practically useless, because
it can't exploit the actual benefit of a linked list structure, where you want
to remove an element added later in a constant time operation. The `RefLinkedList`
implementation solves this problem and makes linked list useful again. See the
following example:

```java
RefList<Integer> list = new RefLinkedList<>();
IntStream.range(0, 100000).forEach(list::add);
RefList.ElementRef<Integer> elementRef = list.addLastGetReference(1234567);
IntStream.range(100001, 200000).forEach(list::add);

// Remove the 1234567 in constant time regardless how many other elements were added:
elementRef.remove();
// Now 1234567 is no longer part of "list"
```

Sometimes when removing an element from a limit capacity queue, you may want to
prevent the addition of new elements for a while. The `ReservablePollingQueue`
allows you that. See for example, where all of the asserts succeed:

```java
ReservablePollingQueue<String> queue = ReservablePollingQueues.createFifoQueue(1);
queue.offer("MyElement");

ReservedElementRef<String> elementRef = queue.pollButKeepReserved();
boolean newElementAdded = queue.offer("NewElement");

assert !newElementAdded;
assert queue.poll() == null;

elementRef.release();
boolean postReleaseElementAdded = queue.offer("PostReleaseElement");

assert postReleaseElementAdded;
assert "PostReleaseElement".equals(queue.poll());
```

Altough this is not generally useful in a single threaded context, it is sometimes
preferred in a producer-consumer model. In that case, you might want to take a look
at the `org.jtrim2.concurrent.collections.TerminableQueue` of the *jtrim-concurrent*
package, which has more advanced support for such a produced-consumer negotiation.

### Core interfaces

- `RefList`: A List of which you can easily and efficiently manipulate its
  elements after adding them.
- `ReservablePollingQueue`: A queue where you can prevent addition of new elements
  even after an element was removed for some time.

### Core classes

- `RefLinkedList`: An implementation of `RefList`.
- `ReservablePollingQueues`: Contains factory methods to create instances of `ReservablePollingQueue`.
