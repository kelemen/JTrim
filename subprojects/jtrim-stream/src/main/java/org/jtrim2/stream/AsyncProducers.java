package org.jtrim2.stream;

/**
 * Defines factory methods for asynchronously populated synchronous producers.
 */
public final class AsyncProducers {
    /**
     * Returns an asynchronously populatable sink and synchronous producer(s) producing the elements
     * put into the sink. It is possible to create multiple producers, in which case it is undefined
     * which element put into the sink will be produced by which producer. However, an element
     * will only be produced by exactly one producer. Producers that are never asked to produce
     * elements can be ignored (i.e. they won't prevent elements to be processed).
     * <P>
     * Note: The returned producers will only return after the sink is marked as
     * {@link AsyncElementSink#finish(Throwable) finished} or if the consumer of any of the producers
     * fail with an exception.
     * <P>
     * See the following example for a trivial use-case:
     * <pre>{@code
     * AsyncProducerRef<String> ref = AsyncProducers.createAsyncSourcedProducer(20);
     * AsyncElementSink<T> sink = ref.getElementSink();
     *
     * myAsyncSource.onReceiveElement(str -> sink.tryPut(Cancellation.UNCANCELABLE_TOKEN, str));
     * myAsyncSource.onClose(exception -> ref.getElementSink().finish(exception));
     *
     * ref.newSeqProducer()
     *     .toFluent()
     *     .withContextFreeConsumer(str -> process(str))
     *     .execute(Cancellation.UNCANCELABLE_TOKEN);
     * }</pre>
     * <P>
     * This method is effectively equivalent to {@code createAsyncSourcedProducer(maxQueueSize, maxQueueSize)}.
     *
     * @param <T> the type of the elements produced
     * @param maxQueueSize the maximum number of outstanding elements the queue storing the elements
     *   of the sink can store. The elements need to be stored in the queue until they were retrieved
     *   by a producer. This argument must be greater than or equal to 1.
     * @return an asynchronously populatable sink and synchronous producer(s) producing the elements
     *   put into the sink. This method never returns {@code null}.
     */
    public static <T> AsyncProducerRef<T> createAsyncSourcedProducer(int maxQueueSize) {
        return createAsyncSourcedProducer(maxQueueSize, maxQueueSize);
    }

    /**
     * Returns an asynchronously populatable sink and synchronous producer(s) producing the elements
     * put into the sink. It is possible to create multiple producers, in which case it is undefined
     * which element put into the sink will be produced by which producer. However, an element
     * will only be produced by exactly one producer. Producers that are never asked to produce
     * elements can be ignored (i.e. they won't prevent elements to be processed).
     * <P>
     * Note: The returned producers will only return after the sink is marked as
     * {@link AsyncElementSink#finish(Throwable) finished} or if the consumer of any of the producers
     * fail with an exception.
     * <P>
     * See the following example for a trivial use-case:
     * <pre>{@code
     * AsyncProducerRef<String> ref = AsyncProducers.createAsyncSourcedProducer(1000, 10);
     * AsyncElementSink<T> sink = ref.getElementSink();
     *
     * myAsyncSource.onReceiveElement(str -> sink.tryPut(Cancellation.UNCANCELABLE_TOKEN, str));
     * myAsyncSource.onClose(exception -> ref.getElementSink().finish(exception));
     *
     * ref.newSeqProducer()
     *     .toFluent()
     *     .withContextFreeConsumer(str -> process(str))
     *     .execute(Cancellation.UNCANCELABLE_TOKEN);
     * }</pre>
     *
     * @param <T> the type of the elements produced
     * @param maxQueueSize the maximum number of outstanding elements the queue storing the elements
     *   of the sink can store. The elements need to be stored in the queue until they were retrieved
     *   by a producer. This argument must be greater than or equal to 1.
     * @param initialQueueCapacity an initial capacity of the queue should reserve initially. Setting
     *   this parameter to any allowed value is only an optimization and carry no defined semantics. This
     *   argument must be greater than or equal to zero, but no greater than {@code maxQueueSize}.
     * @return an asynchronously populatable sink and synchronous producer(s) producing the elements
     *   put into the sink. This method never returns {@code null}.
     */
    public static <T> AsyncProducerRef<T> createAsyncSourcedProducer(int maxQueueSize, int initialQueueCapacity) {
        DefaultAsyncElementSource<T> source = new DefaultAsyncElementSource<>(maxQueueSize, initialQueueCapacity);
        return new AsyncProducerRef<>(source, () -> new AsyncSourceProducer<>(source));
    }

    private AsyncProducers() {
        throw new AssertionError();
    }
}
