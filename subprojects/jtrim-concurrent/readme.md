Generic Concurrency Utilities
=============================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-concurrent:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-concurrent</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-collections"
  - "org.jtrim2:jtrim-utils"


Description
-----------

Contains concurrency utilites covering the following problems:

- Cancellation
- Event handling
- Concurrent collections
- Other utilities

### Cancellation

This is probably the most fundamental part of JTrim as it is used almost
everywhere. The core Java libraries care little about cancellation, the APIs
actually caring about it rely on thread interruption. However, thread
interruption is very hard to use correctly and it is often misused by libraries.
That is, hiding thread interruptions are common place, despite being a wrong
practice. Problems with threads interruptions:

- There is no defined semantics for thread interruptions. They are usually used
  for cancellation but if you want to be on the safe side, you cannot assume in
  general that interrupting a thread you did not create has this semantics.
- The interrupted status is effectively a mutable public global state. This is
  just wrong in almost any coding convention for a good reason.
- You cannot asynchronously detect thread interruptions. You can only check if a
  thread has been interrupted (and clear the interrupted status if you want).
  This means that, if a blocking call needs a method to be called to be
  canceled, you are out of luck.

So, what solution JTrim offers? JTrim uses the concept of `CancellationToken`
which is very similar to the one introduced in *.NET 4*. In fact, if you have
been using this feature in *.NET*, you will find it more familiar than not. For
those not knowing of this *.NET* feature, `CancellationToken` is an object which
singals cancellation requests. You can either poll the token if cancellation has
been requested or register a listener which is to be notified upon cancellation
requests. In general, it is not defined how a `CancellationToken` can transition
to the canceled state (if it can) but in most cases you want to create a
`CancellationSource` which provides a controller which is able to cancel the
associated token. For more detailed information about cancellation, you should
brows the `org.jtrim2.cancel` package. This package also contains classes to
convert thread interruption based APIs to a `CancellationToken` based API
(`CancelableWaits`).

#### Examples

A simple loop polling the `CancellationToken` looks like this:

```java
double sumAll(CancellationToken cancelToken, double[][] src) {
    double result = 0.0;
    for (double[] line : src) {
        cancelToken.checkCanceled();
        for (double x : line) {
            result += x;
        }
    }
    return result;
}
```

If an API requires an external method call to cancel an outstanding operation,
you could consider the following example:

```java
byte[] readBytes(CancellationToken cancelToken, URL src) throws IOException {
    try (InputStream input = src.openStream()) {
        ListenerRef cancelRef = cancelToken.addCancellationListener(() -> {
            try {
                input.close();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        try {
            byte[] result = new byte[1024];
            input.read(result);
            return result;
        } catch (SocketException ex) {
            if (cancelToken.isCanceled()) {
                throw new OperationCanceledException(ex);
            } else {
                throw ex;
            }
        } finally {
            cancelRef.unregister();
        }
    }
}
```

#### Core interfaces

- `CancellationToken`: Detecting cancellation requests.
- `CancellationSource`: Cancelling and detecting cancellation.

#### Core classes

- `Cancellation`: Factory and other helper methods.
- `CancelableWaits`: Converting thread interruption to CancellationToken based
  cancellation.


### Event Handling

Event handlers or listeners are simply asynchronous callbacks. They are usually
used in GUI frameworks but it doesn't have to be this way. For example, JTrim
employs them to provide asynchronous notifications of when an executor
terminates or when a `CancellationToken` gets canceled.

The usual idiom in java for event handlers is to provide a pair of methods:
*addXXX* and *removeXXX* where *addXXX* adds an event handler and *removeXXX*
can remove a previously added event handler. This idiom is incredibly limiting.
First, if you ever had to register multiple different listeners, you should know
that you have to remeber the listeners in separate fields and also unregister
them separatly. Wouldn't it be better to have a single list on which you can
simply iterate over to have each listener unregistered? Tried to chain different
listeners (i.e.: forward events to another event)? Is it really painful? Why?
Mostly, because it is not obvious (and usually not even defined) if *removeXXX*
is based on equals or reference comparison. In fact some implementations are
based on equals and some are on reference comparison. Also, did you ever wonder
what happens if you add the same (again, same in terms of what?) listener
multiple times? You should have, although the expected behaviour is usually
defined for this problem.

How to solve all the above mentioned problems? It is actually quite simple:
Have an *addXXX* method which returns a `ListenerRef` instance where
`ListenerRef` has a method named `unregister`. I guess, I don't have to explain
what will happen when you call `unregister`. Also, this allows simple generic
implementations for dispatching event notifications, saving you from rolling out
your own implementations. The main interface in JTrim which you should look for
is `ListenerManager`. An added benefit is that it is possible to create more
efficient implementations in the JTrim way (you don't need sets and whatnot).

#### Core interface

- `ListenerManager`: Stores event listeners and dispatches events.

#### Core class

- `CopyOnTriggerListenerManager`: A generic implementation of ListenerManager.

### Concurrent collections

Some generic thread-safe collections not available in the JDK.

#### Examples

The `TerminableQueue` can be used to conveniently safely transfer data between producers and consumers, and
communicate if any of the sides go away without the risk of waiting forever. See the code below for example:

```java
TerminableQueue<String> queue = TerminableQueues.withWrappedQueue(ReservablePollingQueues.createFifoQueue(10));
try {
    new Thread(() -> {
        try {
            while (true) {
                String receivedLine = queue.take(Cancellation.UNCANCELABLE_TOKEN);
                System.out.println("New element was received: " + receivedLine);
                // Some code might be here which can fail unexpectedly.
            }
        } catch (TerminatedQueueException ex) {
            System.out.println("The queue was closed and is now empty.");
        } finally {
            queue.shutdown();
        }
    }).start();

    Console console = System.console();
    String line;
    while ((line = console.readLine()) != null) {
        queue.put(Cancellation.UNCANCELABLE_TOKEN, line);
    }
} finally {
    queue.shutdownAndWaitUntilEmpty(Cancellation.UNCANCELABLE_TOKEN);
}
```

#### Core interface

- `TerminableQueue`: Defines a thread-safe queue, where producers and consumers can communicate easily
  that they stopped. The queue also support removing elements, but preventing new elements to be added
  in place of the removed one until the removed element was completely processed.

#### Core class

- `TerminableQueues`: Contains factory methods to create instances of `TerminableQueue`.
