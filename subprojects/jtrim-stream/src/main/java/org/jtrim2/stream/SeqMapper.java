package org.jtrim2.stream;

import java.util.function.Function;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an action mapping a sequence of elements to another sequence of elements. This interface
 * (unlike {@link ElementMapper} provides an opportunity to do something before and after the mapping
 * process.
 * <P>
 * See the following example for a simple implementation:
 * <pre>{@code
 * SeqMapper<String, Integer> mapper = (cancelToken, seqProducer, seqConsumer) -> {
 *   try (RemoteService service = connect()) {
 *     SeqProducer<Integer> mappedProducer = seqProducer
 *         .toFluent()
 *         .mapContextFree((element, consumer) -> {
 *           cancelToken.checkCanceled();
 *           int id = service.nameToCode(element);
 *           consumer.processElement(id);
 *         })
 *         .unwrap();
 *     seqConsumer.consumeAll(cancelToken, mappedProducer);
 *   }
 * };
 * }</pre>
 * The above example code translates each element (assumed to be a product name) to an integer code value
 * using an external service, and ensuring that the service is connected to once, and the connection is
 * properly closed after the processing was done. Note that there is a small flaw in the above implementation:
 * In theory, the {@code seqConsumer} might also request cancellation (for whatever reason), and that is
 * ignored. This can be fixed by manually implementing {@code SeqProducer} which slightly complicates the code.
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of this interface are completely implementation dependent.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be synchronization transparent.
 *
 * @param <T> the type of the elements of the input stream
 * @param <R> the type of the elements this mapper is producing
 *
 * @see ElementMapper
 * @see SeqGroupMapper
 */
public interface SeqMapper<T, R> {
    /**
     * Returns a mapper mapping the input elements to the same elements as the input. This
     * is a one-to-one mapping.
     * <P>
     * The returns mapper is reusable any number of times.
     *
     * @param <T> the type of the input elements, which is also the type of the output
     * @return a mapper mapping the input elements to the same elements as the input. This
     *   method never returns {@code null}.
     */
    public static <T> SeqMapper<T, T> identity() {
        return ElementMappers.identitySeqMapper();
    }

    /**
     * Returns a mapper mapping a stream of {@code Iterable} to a stream with the elements
     * of those {@code Iterable} instances retaining the iteration order. For example,
     * if the input stream is {@code [[0, 1, 2], [3, 4]]}, then the output stream will be
     * {@code [0, 1, 2, 3, 4]} (in this order).
     * <P>
     * The returns mapper is reusable any number of times.
     *
     * @param <T> the type of the elements of the input {@code Iterable} instances, which is also the
     *   same as the output type
     * @param <C> the type of the {@code Iterable} (e.g., {@code List})
     *
     * @return a mapper mapping a stream of {@code Iterable} to a stream with the elements
     *   of those {@code Iterable} instances. This method never returns {@code null}.
     *
     * @see FluentSeqProducer#batch(int) FluentSeqProducer.batch
     */
    public static <T, C extends Iterable<? extends T>> SeqMapper<C, T> flatteningMapper() {
        return ElementMappers.flatteningSeqMapper();
    }

    /**
     * Returns a {@code SeqMapper} instance applying the given {@code ElementMapper} to all
     * elements of the input sequence.
     * <P>
     * The returned mapper is reusability is the same as the mapper given in the argument. Note however,
     * that is not normally feasible to pass a non-reusable mapper.
     *
     * @param <T> the type of the elements of the input stream
     * @param <R> the type of the elements this mapper is producing
     * @param mapper the {@code ElementMapper} to be applied to all input elements. This
     *   argument cannot be {@code null}.
     * @return {@code SeqMapper} instance applying the given {@code ElementMapper} to all
     *   elements of the input sequence. This method never returns {@code null}.
     */
    public static <T, R> SeqMapper<T, R> fromElementMapper(ElementMapper<? super T, ? extends R> mapper) {
        return ElementMappers.contextFreeSeqMapper(mapper);
    }

    /**
     * Returns a {@code SeqMapper} instance applying the given function to all elements
     * of the input sequence. That is, maps each element of the input using the given
     * mapper function.
     * <P>
     * The returned mapper is reusability is the same as the mapper given in the argument. Note however,
     * that is not normally feasible to pass a non-reusable mapper.
     *
     * @param <T> the type of the elements of the input stream
     * @param <R> the type of the elements this mapper is producing
     * @param mapper the function to map all elements of the input. This
     *   argument cannot be {@code null}.
     * @return a {@code SeqMapper} instance applying the given function to all elements
     *   of the input sequence. This method never returns {@code null}.
     */
    public static <T, R> SeqMapper<T, R> oneToOneMapper(Function<? super T, ? extends R> mapper) {
        return fromElementMapper(ElementMapper.oneToOneMapper(mapper));
    }

    /**
     * Maps all elements of the given {@code SeqProducer} to be consumed by the given
     * {@code SeqConsumer}. This method must assume that the provided producer and consumer
     * is no longer usable once this method returns.
     * <P>
     * This method must do the mapping exactly once (unless an exception is raised before it could happen).
     * That is, exactly one call to {@code seqConsumer.consumeAll} and exactly one call to
     * {@code seqProducer.transferAll}.
     * <P>
     * Exceptions thrown by the provided producer or consumer must be propagated to the caller, and must
     * not be wrapped (with the exception of checked exceptions not implementing {@code Exception}).
     *
     * @param cancelToken the cancellation token which should be checked if the processing
     *   is to be canceled before completing the whole processing. There is no guarantee that
     *   the implementation will detect the cancellation request, but a well-behaved implementation
     *   should make a best effort check, if it does some lengthy operation. If cancellation was
     *   detected by this call, then it must respond by throwing an
     *   {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}. This argument
     *   cannot be {@code null}.
     * @param seqProducer the producer producing the input sequence to be processed. This
     *   argument cannot be {@code null}.
     * @param seqConsumer the consumer processing the mapped sequence. This argument cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void mapAll(
            CancellationToken cancelToken,
            SeqProducer<? extends T> seqProducer,
            SeqConsumer<? super R> seqConsumer) throws Exception;

    /**
     * Returns a convenient fluent builder to create a complex mapper starting out from
     * this mapper.
     * <P>
     * Implementation note: This method has an appropriate default implementation, and there is normally
     * no reason to override it.
     *
     * @return a convenient fluent builder to create a complex mapper. This method never returns {@code null}.
     */
    public default FluentSeqMapper<T, R> toFluent() {
        return new FluentSeqMapper<>(this);
    }
}
