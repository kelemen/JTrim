package org.jtrim2.stream;

import java.util.function.Function;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an action mapping zero or more sequence of elements. Aside from being able to map
 * multiple sequences in a different way (as opposed to {@link SeqMapper}), this interface
 * allows to do something before and after the complete processing.
 * <P>
 * See the following example for a simple implementation:
 * <pre>{@code
 * SeqGroupMapper<String, Integer> mapper = (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
 *   try (ServiceLocator locator = openLocator()) {
 *     SeqGroupProducer<Integer> mappedProducer = seqGroupProducer
 *         .toFluent()
 *         .map(nameToCodeMapper(() -> locator.findService()))
 *         .unwrap();
 *     seqGroupConsumer.consumeAll(cancelToken, mappedProducer);
 *   }
 * };
 *
 * SeqMapper<String, Integer> nameToCodeMapper(Supplier<RemoteService> serviceProvider) {
 *   return (cancelToken, seqProducer, seqConsumer) -> {
 *     try (RemoteService service = serviceProvider.get().connect()) {
 *       SeqProducer<Integer> mappedProducer = seqProducer
 *           .toFluent()
 *           .mapContextFree((element, consumer) -> {
 *             cancelToken.checkCanceled();
 *             int id = service.nameToCode(element);
 *             consumer.processElement(id);
 *           })
 *           .unwrap();
 *       seqConsumer.consumeAll(cancelToken, mappedProducer);
 *     }
 *   };
 * }
 * }</pre>
 * The above example code connects to a new service for each sequence, and then uses that
 * to do the name to id conversion
 *
 * <h2>Thread safety</h2>
 * The thread-safety property of this interface are completely implementation dependent.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of this interface are not required to be synchronization transparent.
 *
 * @param <T> the type of the elements of the input stream
 * @param <R> the type of the elements this mapper is producing
 *
 * @see ElementMapper
 * @see SeqMapper
 */
public interface SeqGroupMapper<T, R> {
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
    public static <T> SeqGroupMapper<T, T> identity() {
        return ElementMappers.identitySeqGroupMapper();
    }

    /**
     * Returns a mapper mapping streams of {@code Iterable} instances to a stream with the elements
     * of those {@code Iterable} instances retaining the iteration order. For example,
     * if the input is two groups {@code [[[0, 1, 2], [3, 4]], [[5, 6], [7, 8, 9]]]}, then the output will be
     * {@code [[0, 1, 2, 3, 4], [5, 6, 7, 8, 9]]} (in this order).
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
     * @see FluentSeqGroupProducer#batch(int) FluentSeqGroupProducer.batch
     */
    public static <T, C extends Iterable<? extends T>> SeqGroupMapper<C, T> flatteningMapper() {
        return ElementMappers.flatteningSeqGroupMapper();
    }

    /**
     * Returns a {@code SeqGroupMapper} instance applying the given {@code SeqMapper} to all input sequences.
     * <P>
     * The returned mapper is reusability is the same as the mapper given in the argument.
     *
     * @param <T> the type of the elements of the input sequences
     * @param <R> the type of the elements this mapper is producing
     * @param mapper the {@code SeqMapper} to be applied to all input sequences. This
     *   argument cannot be {@code null}.
     * @return a {@code SeqGroupMapper} instance applying the given {@code SeqMapper} to all input sequences.
     *   This method never returns {@code null}.
     */
    public static <T, R> SeqGroupMapper<T, R> fromMapper(SeqMapper<? super T, ? extends R> mapper) {
        return ElementMappers.contextFreeSeqGroupMapper(mapper);
    }

    /**
     * Returns a {@code SeqGroupMapper} instance applying the given {@code ElementMapper} to all
     * elements of all the input sequences.
     * <P>
     * The returned mapper is reusability is the same as the mapper given in the argument. Note however,
     * that is not normally feasible to pass a non-reusable mapper.
     *
     * @param <T> the type of the elements of the input sequences
     * @param <R> the type of the elements this mapper is producing
     * @param mapper the {@code ElementMapper} to be applied to all input elements. This
     *   argument cannot be {@code null}.
     * @return {@code SeqGroupMapper} instance applying the given {@code ElementMapper} to all
     *   elements of all the input sequences. This method never returns {@code null}.
     */
    public static <T, R> SeqGroupMapper<T, R> fromElementMapper(ElementMapper<? super T, ? extends R> mapper) {
        return ElementMappers.contextFreeSeqGroupMapper(mapper);
    }

    /**
     * Returns a {@code SeqGroupMapper} instance applying the given function to all elements
     * of all the input sequences. That is, maps each element of the input using the given
     * mapper function.
     * <P>
     * The returned mapper is reusability is the same as the mapper given in the argument. Note however,
     * that is not normally feasible to pass a non-reusable mapper.
     *
     * @param <T> the type of the elements of the input sequences
     * @param <R> the type of the elements this mapper is producing
     * @param mapper the function to map all elements of the input. This
     *   argument cannot be {@code null}.
     * @return a {@code SeqGroupMapper} instance applying the given function to all elements
     *   of all the input sequences. This method never returns {@code null}.
     */
    public static <T, R> SeqGroupMapper<T, R> oneToOneMapper(Function<? super T, ? extends R> mapper) {
        return fromElementMapper(ElementMapper.oneToOneMapper(mapper));
    }

    /**
     * Maps all sequences of the given {@code SeqGroupProducer} to be consumed by the given
     * {@code SeqGroupConsumer}. This method must assume that the provided producer and consumer
     * is no longer usable once this method returns.
     * <P>
     * This method must do the mapping exactly once (unless an exception is raised before it could happen).
     * That is, exactly one call to {@code seqGroupConsumer.consumeAll} and exactly one call to
     * {@code seqGroupProducer.transferAll}.
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
     * @param seqGroupProducer the producer producing the input sequences to be processed. This
     *   argument cannot be {@code null}.
     * @param seqGroupConsumer the consumer processing the mapped sequences. This argument cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void mapAll(
            CancellationToken cancelToken,
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqGroupConsumer<? super R> seqGroupConsumer) throws Exception;

    /**
     * Returns a convenient fluent builder to create a complex mapper starting out from
     * this mapper.
     * <P>
     * Implementation note: This method has an appropriate default implementation, and there is normally
     * no reason to override it.
     *
     * @return a convenient fluent builder to create a complex mapper. This method never returns {@code null}.
     */
    public default FluentSeqGroupMapper<T, R> toFluent() {
        return new FluentSeqGroupMapper<>(this);
    }
}
