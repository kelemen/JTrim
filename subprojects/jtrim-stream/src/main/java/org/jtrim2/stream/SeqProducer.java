package org.jtrim2.stream;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ArraysEx;
import org.jtrim2.collections.ForEachable;

/**
 * Defines a source of a sequence of elements. If you wish to provide multiple sequences of elements,
 * then consider using {@link SeqGroupProducer}. The producer may or may not be reusable depending on
 * the implementation. If the producer is defined to be reusable, then it should produce the same
 * elements every time it is requested for the sequence (of course, if the source is an external source
 * that is not always enforceable).
 * <P>
 * Note that this interface can be conveniently implemented by a lambda. For example:
 * <pre>{@code
 * Path srcFile = ...;
 * SeqProducer<String> producer = (cancelToken, consumer) -> {
 *   try (BufferedReader source = Files.newBufferedReader(srcFile, StandardCharsets.UTF_8)) {
 *     String line;
 *     while ((line = source.readLine()) != null) {
 *       cancelToken.checkCanceled();
 *       consumer.processElement(transformJob(line, "reader"));
 *     }
 *   }
 * };
 * }</pre>
 * The above example code provides the lines of a file in order, while properly opening the
 * file exactly once before processing, and the closing it as appropriate.
 *
 * <h2>Thread safety</h2>
 * The thread-safety property of this interface are completely implementation dependent.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of this interface are not required to be synchronization transparent.
 *
 * @param <T> the type of the produced elements
 *
 * @see SeqGroupProducer
 */
public interface SeqProducer<T> {
    /**
     * Returns a producer producing zero elements (i.e., it will never make a call to the consumer).
     * <P>
     * The returned producer is reusable any number of times.
     *
     * @param <T> the type of the produced elements
     * @return a producer producing zero elements (i.e., it will never make a call to the consumer).
     *   This method never returns {@code null}.
     */
    public static <T> SeqProducer<T> empty() {
        return ElementProducers.emptySeqProducer();
    }

    /**
     * Returns a producer producing the elements of the {@code Iterable} instances produced by
     * the given producer. For example, if the given producer produces is {@code [[0, 1, 2], [3, 4]]},
     * then the returned producer will produce {@code [0, 1, 2, 3, 4]}.
     * <P>
     * The reusability of the returned producer is the same as the producer given in the argument.
     *
     * @param <T> the type of the element produced by the returned producer
     * @param src the source producer producing {@code Iterable} instances to be flattened.
     *   This argument cannot be {@code null}.
     * @return a producer producing the elements of the {@code Iterable} instances produced by
     *   the given producer. This method never returns {@code null}.
     *
     * @see FluentSeqProducer#apply(java.util.function.Function) FluentSeqProducer.apply
     * @see FluentSeqProducer#batch(int) FluentSeqProducer.batch
     */
    public static <T> SeqProducer<T> flatteningProducer(
            SeqProducer<? extends Iterable<? extends T>> src) {

        return ElementProducers.flatteningSeqProducer(src);
    }

    /**
     * Returns a producer producing the elements given in the argument. If you explicitly pass
     * an array as an argument, then the array is copied, and changes to the array will not affect
     * the returned producer.
     * <P>
     * The returned producer is reusable any number of times.
     *
     * @param <T> the type of the produced elements
     * @param src the elements to be produced by the returned producer (in order). This
     *   argument cannot be {@code null}, and since these streams do not support {@code null} elements,
     *   it must not contain {@code null} elements.
     * @return a producer producing the elements given in the argument. This
     *   method never returns {@code null}.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> SeqProducer<T> copiedArrayProducer(T... src) {
        return arrayProducer(src.clone());
    }

    /**
     * Returns a producer producing the elements given in the argument. The given array is not copied,
     * so changes to the passed array will affect the returned producer.
     * <P>
     * The returned producer is reusable any number of times.
     *
     * @param <T> the type of the produced elements
     * @param src the elements to be produced by the returned producer (in order). This
     *   argument cannot be {@code null}, and since these streams do not support {@code null} elements,
     *   it must not contain {@code null} elements.
     * @return a producer producing the elements given in the argument. This
     *   method never returns {@code null}.
     */
    public static <T> SeqProducer<T> arrayProducer(T[] src) {
        return iterableProducer(ArraysEx.viewAsList(src));
    }

    /**
     * Returns a producer producing the elements given in the argument. The given {@code Iterable}
     * is not copied, so changes to the passed {@code Iterable} will affect the returned producer.
     * <P>
     * The returned producer is reusable any number of times.
     *
     * @param <T> the type of the produced elements
     * @param src the elements to be produced by the returned producer (in order). This
     *   argument cannot be {@code null}, and since these streams do not support {@code null} elements,
     *   it must not contain {@code null} elements.
     * @return a producer producing the elements given in the argument. This
     *   method never returns {@code null}.
     */
    public static <T> SeqProducer<T> iterableProducer(Iterable<? extends T> src) {
        return forEachableProducer((ForEachable<T>) src::forEach);
    }

    /**
     * Returns a producer producing the elements given in the argument. The given {@code ForEachable}
     * is not copied, so changes to the passed {@code ForEachable} will affect the returned producer.
     * <P>
     * The returned producer is reusable any number of times.
     *
     * @param <T> the type of the produced elements
     * @param src the elements to be produced by the returned producer (in order). This
     *   argument cannot be {@code null}, and since these streams do not support {@code null} elements,
     *   it must not contain {@code null} elements.
     * @return a producer producing the elements given in the argument. This
     *   method never returns {@code null}.
     */
    public static <T> SeqProducer<T> forEachableProducer(ForEachable<? extends T> src) {
        return ElementProducers.forEachableSeqProducer(src);
    }

    /**
     * Passes all the underlying elements to the provided consumer in order. Note that an empty producer
     * will never invoke the given consumer.  This method must assume that the provided consumer is no
     * longer usable once this method returns.
     * <P>
     * Exceptions thrown by the provided {@code consumer} must be propagated to the caller, and must
     * not be wrapped (with the exception of checked exceptions not implementing {@code Exception}).
     *
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a well-behaved implementation
     *   should make a best effort check, if it does some lengthy operation. If cancellation was
     *   detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param consumer the consumer to which the elements are to be passed to in the order of underlying
     *   sequence. This argument cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void transferAll(CancellationToken cancelToken, ElementConsumer<? super T> consumer) throws Exception;

    /**
     * Returns a convenient fluent builder to create a complex producer starting out from
     * this producer.
     * <P>
     * Implementation note: This method has an appropriate default implementation, and there is normally
     * no reason to override it.
     *
     * @return a convenient fluent builder to create a complex producer. This method
     *   never returns {@code null}.
     */
    public default FluentSeqProducer<T> toFluent() {
        return new FluentSeqProducer<>(this);
    }
}
