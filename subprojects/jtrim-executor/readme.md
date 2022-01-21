Executors
=========

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-executor:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-executor</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-concurrent"
  - "org.jtrim2:jtrim-collections"
    - "org.jtrim2:jtrim-utils"


Description
-----------

If you are familiar with the concurrency API introduced in Java 5, you might
know that there is an executor framework in Java. So, what's the reason to have
another one, you may ask? Though the API in Java is one of the best I have seen,
I did have some frustration with it. First of all, cancellation in the framework
of Java is inconvenient for non-trivial tasks. Usually you have to resort to
thread interruptions and this have problems described in the *jtrim-concurrent*
module What was more frustrating to me in the API of Java is that you did not
have the ability to do something after a task has been terminated (in any way,
either by cancellation or normally). You could wait for the `Future` returned by
the `ExecutorService` but that is not asynchrous and starting a new thread just
for waiting is more than inefficient. Also, it is not fool proof either, because
of the `shutdownNow` method, it is perfectly possible that a task never
transitions to any completed state. In fact, since tasks were usually wrapped
(it was necessary due to the design of the API), there is little you could do
with the result of the `shutdownNow` method: You could not save them and obviously
you probably did not want to run them (why did you call `shutdownNow` in the
first place?). Although if you know the internals of the executor which you shut
down, you could attempt to cast them to something (e.g.: `Future`) but this
seems a very fragile thing to me.

JTrim aims to solve the problems mentioned in the previous paragraph.
Cancellation mechanism is greatly simplified by relying on the cancellation API
of JTrim. Instead of returning a `Future`, executors of JTrim return an instance
of `CompletionStage` (added in Java 8), what you can use to execute tasks after
the completion of the asynchrously executed task conveniently. The two main
interfaces for this framework are `TaskExecutor` and `TaskExecutorService`. Note
that, although `TaskExecutor` can be similary simple to implement as the
`Executor` interface of the JDK, `TaskExecutor` is a lot more powerful. In most
cases - when you do not want shutdown - you can get away with a `TaskExecutor`
instead of a more powerful `TaskExecutorService`.

Although the interfaces allow for similar implementations (both in terms of
performance and features) to what the JDK contains, you might be wondering how
the actual implementations in JTrim compare to the ones provided by the JDK.
Implementations in JTrim were designed to answer promptly to cancellation
requests and to release references to tasks as soon as possible. So if you
expect tasks to be canceled often, you will find implementations in JTrim
better. However, prompt cancellation incurs some overhead and therefore
implementations in JTrim usually have lower performance (also less concurrent)
than executors in the JDK. It is however always possible to create
implementations having similar performance that of the classes in the JDK in
expense of the more reliable responses to cancellation requests. A major benefit
of JTrim implementations is that there are more kinds executors in JTrim than in
the JDK. A notable implementation is which forwards task execution to a given
executor, but regardless of the given executor, it will always execute the
submitted tasks in the same order as they were submitted.

### Core interfaces

- `TaskExecutor`: Replacement of the `Executor` interface of the JDK. This
  interface extends the `Executor` interface of the JDK.
- `TaskExecutorService`: Replacement of the `ExecutorService` interface of the
  JDK.
- `UpdateTaskExecutor`: For executing tasks where only the one submitted last is
  useful.

### Core class

- `ExecutorConverter`: Converting to and from the JDK and JTrim executors.
- `GenericUpdateTaskExecutor`: A generic implementation of `UpdateTaskExecutor`
  backed by an `Executor`.
- `TaskExecutors`: Factory methods for useful executor implementations.
- `ThreadPoolBuilder`: A builder class to create thread pooling executors
  similar to the `ThreadPoolExecutor` of the JDK.

### Example

An example of usage of `TaskExecutorService`:

```java
TaskExecutorService executor = ThreadPoolBuilder.create("My-Thread-Pool", config -> {
    config.setMaxThreadCount(8);
    config.setMaxQueueSize(100);
    // Executes tasks recursively when the queue is full.
    config.setFullQueueHandlerToFallback(SyncTaskExecutor.getSimpleExecutor());
});
try {
    CancellationSource cancel = Cancellation.createCancellationSource();

    // ...

    executor.execute(cancel.getToken(), (cancelToken) -> {
        System.out.println("Executed asynchronously.");
    }).thenAccept((result) -> {
        System.out.println("Executed after the task.");
    }).exceptionally(AsyncTasks::expectNoError);

    // ...
} finally {
    executor.shutdown();
}
```

An example of usage of `UpdateTaskExecutor` is shown below. The benefit of
`UpdateTaskExecutor` in this situation is that if the UI updates the text
property slower than you are attempting to update it, the previous (obsolete)
text updates are discarded (as they would simply be overwritten anyway).

```java
void doSomethingWithProgress(javafx.scene.text.Text progressText) {
    UpdateTaskExecutor textUpdater = new GenericUpdateTaskExecutor(Platform::runLater);

    for (int i = 0; i < 100; i++) {
        String text = "Text " + i;
        textUpdater.execute(() -> progressText.setText(text));

        // ... Do something
    }
}
```
