Synchronous Producer-Consumer Management
========================================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-stream:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-stream</artifactId>
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

The goal of this library is to allow simple custom implementation of synchronous
producers and consumers, yet allow combining simple producers to create a
more complex one. For example, it is possible to make any producer move
processing into a separate thread allowing the producer and consumer run
concurrently. This leads to better resource utilization, if the producer and
consumer are using different resources.


### Producer

Unlike Java's `Stream` instance, where the elements are generally unorganized
(except for a few flags). This library organizes the stream of elements into
separate (zero or more) sequences, and then within sequences the processing is
always sequential. For example consider the following implementation reading
the lines of a file:

```java
// srcFile is an instance of Path.
SeqProducer<String> producer = (cancelToken, consumer) -> {
    try (BufferedReader source = Files.newBufferedReader(srcFile, UTF_8)) {
        String line;
        while ((line = source.readLine()) != null) {
            cancelToken.checkCanceled();
            consumer.processElement(line);
        }
    }
};
```

Notice that the implementation of a stream is natural, unlike if you had
to implement an `Iterable` (let alone a `Stream`). Also, an `Iterable` does
not provide a natural way to open and release resources (such as files). Even
though `Stream` allows closing, it is very easy to misuse.

The above implementation produces only a single sequence. Suppose you want to
iterate over the content of multiple files, but have the content of each file
as a separate sequence. Assume that there is a method
`SeqProducer<String> newLineReaderProducer(Path srcFile)` method creating the
above single sequence producer. Then see the following example:

```java
SeqGroupProducer<String> seqGroupProducer = (cancelToken, seqConsumer) -> {
    try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.txt")) {
        for (Path file : files) {
            seqConsumer.consumeAll(cancelToken, newLineReaderProducer(file));
        }
    }
};
```


### Mappers

You can also define mappers independently of producers and consumers. For this
you have 3 options. The first and simplest option is, if you don't need any
closeable resource, then use `ElementMapper`:

```java
ElementMapper<String, Integer> elementMapper = (element, consumer) -> {
    cancelToken.checkCanceled();
    int id = service.nameToCode(element);
    consumer.processElement(id);
};
```

The above code assumes that you have a `cancelToken` of type `CancellationToken`
and a `service` object mapping names to integer IDs. Notice that the mapper
can call the `processElement` as many times as it would like to (including
zero), meaning that the mapper is capable to filter the list or act as a generally
flat map operation.

If you need to open and close the service, then you can implement `SeqMapper`
instead:

```java
SeqMapper<String, Integer> mapper = (cancelToken, seqProducer, seqConsumer) -> {
    try (RemoteService service = connect()) {
        SeqProducer<Integer> mappedProducer = seqProducer
                .toFluent()
                .mapContextFree((element, consumer) -> {
                    cancelToken.checkCanceled();
                    int id = service.nameToCode(element);
                    consumer.processElement(id);
                })
                .unwrap();
        seqConsumer.consumeAll(cancelToken, mappedProducer);
    }
};
```

There is also a `SeqGroupMapper`, if you need to map multiple sequences, and
need a common resource. For example, you might want to open the above name to ID
mapper service only once, not separately for each sequence. The implementation
for that is effectively the same as above, but you receive a `SeqGroupConsumer`
and a `SeqGroupProducer`.


## Consumers

Though the API supports Java's `Collector` to do something with the elements of
the producer, there is also a way to allow easy resource usage when consuming
elements of producers. For example, a simple implementation might output the
objects into a separate line of a destination file (`destFile`):

```java
SeqConsumer<Object> consumer = (cancelToken, producer) -> {
    try (Writer writer = Files.newBufferedWriter(destFile, UTF_8)) {
        producer.transferAll(cancelToken, job -> {
            cancelToken.checkCanceled();
            writer.write(job + "\n");
        });
    }
};
```

Of course, you might want to write multiple sequences into separate files. In
this case, we have to modify the above code a little, and provide the following
method:

```java
SeqConsumer<Object> newLineWriterConsumer(Supplier<Path> destRef) {
    return (cancelToken, producer) -> {
        try (Writer writer = Files.newBufferedWriter(destRef.get(), UTF_8)) {
            producer.transferAll(cancelToken, job -> {
                cancelToken.checkCanceled();
                writer.write(job + "\n");
            });
        }
    };
}
```

Using the above method we can now conveniently achieve our goal:

```java
SeqGroupConsumer<Object> seqGroupConsumer = (cancelToken, producer) -> {
    Files.createDirectories(destDir);
    AtomicInteger idRef = new AtomicInteger(0);
    Supplier<String> fileNameProvider = () -> idRef.getAndIncrement() + ".out";
    producer.transferAll(
            cancelToken,
            newLineWriterConsumer(() -> destDir.resolve(fileNameProvider.get()))
    );
    Files.createFile(destDir.resolve("signal"));
};
```

The above code also creates the destination directory once before processing any
sequence, and creates an empty signal file after all processing was done.


#### Examples

Note that the producers and consumers declared in the previous sections are
already usable without any code:

```java
consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, producer);
```

It is interesting to note that all the above codes are completely custom and
only implement interface, not using any actual code from the library, yet just
by virtue of the interfaces are fully usable. However, let's create something
more complex. As a first step, let's apply out mapper:

```java
producer
        .toFluent()
        .map(mapper)
        .withConsumer(consumer)
        .execute(Cancellation.UNCANCELABLE_TOKEN);
```

The above code will mapp each line of the input file to an ID and output
that ID to the output file (each ID in a separate line).

Note that in this case, the way the processing goes is that the producer reads
one line, then the mapper is applied, then the consumer outputs the ID to the
file, and this repeats until the whole input file was read. That is, while the
mapper is running no reading, nor writing is done, which is inefficient
resource utilization. Let's fix this:

```java
producer
        .toFluent()
        .toBackground("mapper-executor", 0)
        .map(mapper)
        .toBackground("consumer-executor", 0)
        .withConsumer(consumer)
        .execute(Cancellation.UNCANCELABLE_TOKEN);
```

The above code now better utilizes the available resources as all 3 steps
can run parallel, and the total processing time will be the lowest of the 3
(as opposed to the sum of them in the original code).

Note however that we can do even better! The problem with the above is that
putting elements to be processed on a separate thread has a constant overhead
independent of what object we are putting there. What we can do is to batch
multiple objects together and put these batches as a single list into the
background:

```java
producer
        .toFluent()
        .batch(1000)
        .toBackground("mapper-executor", 0)
        .apply(SeqProducer::flatteningProducer)
        .map(mapper)
        .batch(1000)
        .toBackground("consumer-executor", 0)
        .apply(SeqProducer::flatteningProducer)
        .withConsumer(consumer)
        .execute(Cancellation.UNCANCELABLE_TOKEN);
```

Notice that even though we did not touch the initial simple producer and
consumer implementations, we could still create a powerful processing flow with
relative ease. Of course, in the real world, you might even need to process
the batches directly (e.g., to insert the objects into a table efficiently), but
that is a small detail at this point.


#### Core interfaces

- `SeqProducer`: Represents a simple single sequence producer.
- `SeqGroupProducer`: Represents a producer of zero or more sequences.
- `ElementMapper`: Represents a mapper mapping a single element into zero or
   more other elements.
- `SeqMapper`: Represents a mapper mapping a single sequence to another
   sequence.
- `SeqGroupMapper`: Represents a mapper mapping zero or more sequences to zero
  or more sequences.
- `ElementConsumer`: Represents a consumer processing a single element.
- `SeqConsumer`: Represents a consumer processing a single sequence.
- `SeqGroupConsumer`: Represents a consumer processing zero or more sequences.

#### Core classes

- `FluentSeqProducer`: A fluent style builder for `SeqProducer`.
- `FluentSeqGroupProducer`: A fluent style builder for `SeqGroupProducer`.
- `FluentSeqMapper`: A fluent style builder for `SeqMapper`.
- `FluentSeqGroupMapper`: A fluent style builder for `SeqMapper`.
- `FluentSeqConsumer`: A fluent style builder for `SeqMapper`.
- `FluentSeqGroupConsumer`: A fluent style builder for `SeqGroupConsumer`.
