package org.jtrim2.stream;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines an asynchronously populatable, but synchronously processed producer.
 * That is, an asynchronously populatable sink which provides the source for the
 * synchronous producers via a queue.
 * <P>
 * Note: Usually you want to create instances of this class via the factory methods in
 * {@link AsyncProducers}.
 *
 * <h2>Thread safety</h2>
 * Instances of this class can be shared by multiple threads safely, and its
 * methods can be called concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, so they can be
 * called in any context (e.g.: while holding a lock).
 *
 * @param <T> the type of the produced elements
 *
 * @see AsyncProducers
 */
public final class AsyncProducerRef<T> {
    private final AsyncElementSink<T> elementSink;
    private final Supplier<SeqProducer<T>> producerFactory;

    /**
     * Creates a new instance with the given asynchronously populatable sink providing the
     * data for the given producers (created by the give factory).
     *
     * @param elementSink the asynchronously populatable sink providing the data for the
     *   synchronous producers. This argument will be returned as is by the {@link #getElementSink()} method.
     *   This argument cannot be {@code null}.
     * @param producerFactory the factory creating producers providing the elements put into the sink.
     *   All producers share the same sink. This argument will be returned as is by the {@link #getProducerFactory()}
     *   method. This argument cannot be {@code null}, and the factory may not return {@code null}.
     *
     * @see AsyncProducers
     */
    public AsyncProducerRef(AsyncElementSink<T> elementSink, Supplier<SeqProducer<T>> producerFactory) {
        this.elementSink = Objects.requireNonNull(elementSink, "elementSink");
        this.producerFactory = Objects.requireNonNull(producerFactory, "producerFactory");
    }

    /**
     * Returns the asynchronously populatable sink providing the data for the synchronous producers.
     *
     * @return the asynchronously populatable sink providing the data for the synchronous producers.
     *   This method never returns {@code null}.
     */
    public AsyncElementSink<T> getElementSink() {
        return elementSink;
    }

    /**
     * Returns the factory creating producers providing the elements put into the sink.
     * All producers created by the returned factory will share the {@link #getElementSink() sink},
     * and you can run any number of them in parallel to process the elements put into the sink
     * faster.
     *
     * @return the factory creating producers providing the elements put into the sink.
     *   This method never returns {@code null}.
     */
    public Supplier<SeqProducer<T>> getProducerFactory() {
        return producerFactory;
    }

    private Supplier<SeqProducer<T>> getSafeProducerFactory() {
        Supplier<SeqProducer<T>> producerFactoryCapture = producerFactory;
        return () -> Objects.requireNonNull(producerFactoryCapture.get(), "producerFactory.get()");
    }

    /**
     * Returns a new producer created by the {@link #getProducerFactory() producer factory}.
     *
     * @return a new producer created by the {@link #getProducerFactory() producer factory}.
     *   This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the producer factory returns {@code null}
     */
    public SeqProducer<T> newSeqProducer() {
        return Objects.requireNonNull(producerFactory.get(), "producerFactory.get()");
    }

    /**
     * Returns a new producer producing elements put into the {@link #getElementSink() sink}
     * on the given number of threads. One of the processing thread will be the thread on
     * which the producer is called, and only one less will run in the provided executor.
     * Note: It is assumed that the given executor is able to run {@code maxThreadCount - 1}
     * tasks concurrently. Note that it is not error if the given executor is unable to
     * execute tasks (except that it has to be able to eventually complete scheduled tasks
     * to it).
     *
     * @param executor the executor on which the background processing is done. Note
     *   that one worker will run synchronously, so if you've specified 1 for {@code maxThreadCount},
     *   then the executor will not be used. This argument cannot be {@code null}.
     * @param maxThreadCount the number of concurrent workers processing the elements
     *   put into the sink. This argument must greater than or equal to 1.
     * @return a new producer producing elements put into the {@link #getElementSink() sink}
     *   on the given number of threads. This method never returns {@code null}.
     */
    public SeqGroupProducer<T> newSeqGroupProducer(TaskExecutor executor, int maxThreadCount) {
        Objects.requireNonNull(executor, "executor");
        ExceptionHelper.checkArgumentInRange(maxThreadCount, 1, Integer.MAX_VALUE, "maxThreadCount");

        int extraThreadCount = maxThreadCount - 1;
        if (extraThreadCount == 0) {
            return newSeqProducer().toFluent().toSingleGroupProducer().unwrap();
        }

        Supplier<SeqProducer<T>> producerFactoryCapture = getSafeProducerFactory();

        return (cancelToken, seqConsumer) -> {
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            BackgroundWorkerManager workerManager = new BackgroundWorkerManager(
                    executor,
                    Tasks.noOpTask(),
                    failure -> errorRef.compareAndSet(null, failure)
            );

            workerManager.startWorkers(cancelToken, extraThreadCount, taskCancelToken -> {
                seqConsumer.consumeAll(cancelToken, producerFactoryCapture.get());
            });

            try {
                seqConsumer.consumeAll(cancelToken, producerFactoryCapture.get());
            } catch (Throwable ex) {
                errorRef.compareAndSet(null, ex);
            }

            workerManager.waitForWorkers();
            ExceptionHelper.rethrowCheckedIfNotNull(errorRef.get(), Exception.class);
        };
    }
}
