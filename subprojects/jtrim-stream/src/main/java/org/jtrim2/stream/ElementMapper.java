package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Defines an action mapping a single element of a stream to zero or more elements.
 * Note that mapping to zero elements is effectively a filtering operation, so there
 * is no special filtering operation. The simplest way to create a filtering mapper
 * is by calling the {@link #filteringMapper(Predicate) filteringMapper} method.
 * <P>
 * This interface can often be implemented as a lambda. For example:
 * <pre>{@code
 * ElementMapper<Integer, String> mapper = (element, consumer) -> {
 *   consumer.processElement("a." + element);
 *   consumer.processElement("b." + element);
 * };
 * }</pre>
 * The above example maps each element of the input stream to exactly two other elements.
 * <P>
 * Note that although usually a mapper is side-effect free, this interface does not impose
 * such restriction on its implementations. That is, users of this interface can't assume
 * that they can call the mapper on the same element multiple times.
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
 * @see SeqMapper
 * @see SeqGroupMapper
 */
public interface ElementMapper<T, R> {
    /**
     * Returns a mapper mapping the input element to the same element as the input. This
     * is a one-to-one mapping.
     *
     * @param <T> the type of the input element, which is also the type of the output
     * @return a mapper mapping the input element to the same element as the input. This
     *   method never returns {@code null}.
     */
    public static <T> ElementMapper<T, T> identity() {
        return ElementMappers.identityMapper();
    }

    /**
     * Returns a mapper mapping an {@code Iterable} to as many elements as many the mapped
     * {@code Iterable} has, and calling the consumer with each element separately in iteration order.
     *
     * @param <T> the type of the elements of the input {@code Iterable}, which is also the
     *   same as the output type
     * @param <C> the type of the {@code Iterable} (e.g., {@code List})
     *
     * @return a mapper mapping an {@code Iterable} to as many elements as many the mapped. This
     *   method never returns {@code null}.
     */
    public static <T, C extends Iterable<? extends T>> ElementMapper<C, T> flatteningMapper() {
        return ElementMappers.flatteningMapper();
    }

    /**
     * Returns a mapper mapping input elements to themselves if the given condition is {@code true},
     * and to nothing, if it is {@code false}.
     * <P>
     * If the provided filter is side-effect free, then the returned mapper is also side-effect free.
     *
     * @param <T> the type of the input element, which is also the type of the output
     * @param filter the filter to test if the element should be kept or not. If this filter
     *   returns {@code false}, then the element is removed, otherwise it is kept. This
     *   argument cannot be {@code null}.
     * @return a mapper mapping input elements to themselves according to the given filter.
     *   This method never returns {@code null}.
     */
    public static <T> ElementMapper<T, T> filteringMapper(Predicate<? super T> filter) {
        Objects.requireNonNull(filter, "filter");
        return (element, consumer) -> {
            if (filter.test(element)) {
                consumer.processElement(element);
            }
        };
    }

    /**
     * Returns a simple mapper mapping each element of the input stream to exactly one other element.
     * <P>
     * If the provided mapper action is side-effect free, then the returned mapper is also side-effect free.
     *
     * @param <T> the type of the input element
     * @param <R> the type of the elements this mapper is producing
     * @param mapper the function mapping the elements of the input stream. Note that streams are
     *   not allowed to contain {@code null} values, so this function does not need to handle
     *   {@code null} elements, and may not return {@code null}. Also, this argument cannot be {@code null}.
     * @return a simple mapper mapping each element of the input stream to exactly one other element
     *   according to the given function. This method never returns {@code null}.
     */
    public static <T, R> ElementMapper<T, R> oneToOneMapper(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return (element, consumer) -> {
            R mappedElement = mapper.apply(element);
            consumer.processElement(mappedElement);
        };
    }

    /**
     * Maps the given element to zero or more elements by passing them to the given consumer.
     * The given consumer might be called any number of times (including zero). However, this method
     * must assume that the provided consumer is no longer usable once this method returns. That is,
     * passing the new elements to the consumer must happen synchronously with this method call.
     * <P>
     * Exceptions thrown by the provided {@code consumer} must be propagated to the caller, and must
     * not be wrapped (with the exception of checked exceptions not implementing {@code Exception}).
     *
     * @param element the element to be mapped. This argument cannot be {@code null}.
     * @param consumer the consumer to which the mapped elements are to be passed.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation of the
     *   stream processing was detected by this method call. Note that although this method
     *   does not accept a {@link org.jtrim2.cancel.CancellationToken CancellationToken} for the
     *   sake of simplicity, the context usually has one.
     * @throws Exception thrown if there was a processing failure. This usually means that the whole
     *   processing of the stream is to be discontinued.
     */
    public void map(T element, ElementConsumer<? super R> consumer) throws Exception;
}
