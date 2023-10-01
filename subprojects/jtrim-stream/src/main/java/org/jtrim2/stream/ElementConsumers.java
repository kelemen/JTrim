package org.jtrim2.stream;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class ElementConsumers {
    public static <T> ElementConsumer<T> noOpConsumer() {
        return element -> { };
    }

    public static <T> SeqConsumer<T> drainingSeqConsumer() {
        return (cancelToken, seqProducer) -> seqProducer.transferAll(cancelToken, noOpConsumer());
    }

    public static <T> SeqGroupConsumer<T> drainingSeqGroupConsumer() {
        return (cancelToken, seqGroupProducer) -> seqGroupProducer.transferAll(cancelToken, drainingSeqConsumer());
    }

    private static <T> ElementConsumer<T> castCustomConsumer(ElementConsumer<? super T> consumer) {
        return consumer::processElement;
    }

    private static <T> ElementConsumer<T> castConsumer(ElementConsumer<? super T> consumer) {
        if (consumer == noOpConsumer()) {
            return noOpConsumer();
        } else {
            return castCustomConsumer(consumer);
        }
    }

    private static <T> SeqConsumer<T> castCustomSeqConsumer(SeqConsumer<? super T> seqConsumer) {
        return seqConsumer::consumeAll;
    }

    public static <T> SeqConsumer<T> castSeqConsumer(SeqConsumer<? super T> seqConsumer) {
        if (seqConsumer == drainingSeqConsumer()) {
            return drainingSeqConsumer();
        } else {
            return castCustomSeqConsumer(seqConsumer);
        }
    }

    private static <T> SeqGroupConsumer<T> castCustomSeqGroupConsumer(SeqGroupConsumer<? super T> seqGroupConsumer) {
        return seqGroupConsumer::consumeAll;
    }

    public static <T> SeqGroupConsumer<T> castSeqGroupConsumer(SeqGroupConsumer<? super T> seqGroupConsumer) {
        if (seqGroupConsumer == drainingSeqGroupConsumer()) {
            return drainingSeqGroupConsumer();
        } else {
            return castCustomSeqGroupConsumer(seqGroupConsumer);
        }
    }

    public static <T> SeqConsumer<T> contextFreeSeqConsumer(ElementConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer");

        if (consumer == noOpConsumer()) {
            return drainingSeqConsumer();
        }

        return (cancelToken, seqProducer) -> seqProducer.transferAll(cancelToken, consumer);
    }

    public static <T> SeqGroupConsumer<T> contextFreeSeqGroupConsumer(SeqConsumer<? super T> seqConsumer) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        if (seqConsumer == drainingSeqConsumer()) {
            return drainingSeqGroupConsumer();
        }

        return (cancelToken, seqGroupProducer) -> seqGroupProducer.transferAll(cancelToken, seqConsumer);
    }

    public static <T> SeqGroupConsumer<T> contextFreeSeqGroupConsumer(ElementConsumer<? super T> consumer) {
        return contextFreeSeqGroupConsumer(contextFreeSeqConsumer(consumer));
    }

    private static <T> SeqConsumer<T> toSingleShotSeqConsumer(SeqConsumer<? super T> seqConsumer) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        AtomicBoolean started = new AtomicBoolean(false);
        return (cancelToken, seqProducer) -> {
            if (!started.compareAndSet(false, true)) {
                throw new IllegalStateException("This consumer cannot process multiple groups.");
            }
            seqConsumer.consumeAll(cancelToken, seqProducer);
        };
    }

    public static <T> SeqGroupConsumer<T> toSingleShotSeqGroupConsumer(SeqConsumer<? super T> seqConsumer) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        return (cancelToken, producer) -> {
            producer.transferAll(cancelToken, toSingleShotSeqConsumer(seqConsumer));
        };
    }

    public static <T> ElementConsumer<T> toSynchronizedConsumer(ElementConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer");

        if (consumer == noOpConsumer()) {
            return noOpConsumer();
        }

        Lock consumerLock = new ReentrantLock();
        return element -> {
            consumerLock.lock();
            try {
                consumer.processElement(element);
            } finally {
                consumerLock.unlock();
            }
        };
    }

    public static <T> ElementConsumer<T> concatConsumers(
            ElementConsumer<? super T> consumer1,
            ElementConsumer<? super T> consumer2) {

        Objects.requireNonNull(consumer1, "consumer1");
        Objects.requireNonNull(consumer2, "consumer2");

        if (consumer1 == noOpConsumer()) {
            return castConsumer(consumer2);
        }
        if (consumer2 == noOpConsumer()) {
            return castCustomConsumer(consumer1);
        }

        return element -> {
            consumer1.processElement(element);
            consumer2.processElement(element);
        };
    }

    public static <T> SeqConsumer<T> concatSeqConsumers(
            SeqConsumer<? super T> seqConsumer1,
            SeqConsumer<? super T> seqConsumer2) {

        Objects.requireNonNull(seqConsumer1, "seqConsumer1");
        Objects.requireNonNull(seqConsumer2, "seqConsumer2");

        if (seqConsumer1 == drainingSeqConsumer()) {
            return castSeqConsumer(seqConsumer2);
        }
        if (seqConsumer2 == drainingSeqConsumer()) {
            return castCustomSeqConsumer(seqConsumer1);
        }

        return (cancelToken, producer) -> {
            seqConsumer1.consumeAll(cancelToken, ElementProducers.postPeekedSeqProducer(producer, seqConsumer2));
        };
    }

    public static <T> SeqGroupConsumer<T> concatSeqGroupConsumers(
            SeqGroupConsumer<? super T> seqGroupConsumer1,
            SeqGroupConsumer<? super T> seqGroupConsumer2) {

        Objects.requireNonNull(seqGroupConsumer1, "seqGroupConsumer1");
        Objects.requireNonNull(seqGroupConsumer2, "seqGroupConsumer2");

        if (seqGroupConsumer1 == drainingSeqGroupConsumer()) {
            return castSeqGroupConsumer(seqGroupConsumer2);
        }
        if (seqGroupConsumer2 == drainingSeqGroupConsumer()) {
            return castCustomSeqGroupConsumer(seqGroupConsumer1);
        }

        return (cancelToken, producer) -> {
            seqGroupConsumer1.consumeAll(cancelToken, (producerCancelToken1, consumer1) -> {
                seqGroupConsumer2.consumeAll(producerCancelToken1, (producerCancelToken2, consumer2) -> {
                    producer.transferAll(producerCancelToken2, concatSeqConsumers(consumer1, consumer2));
                });
            });
        };
    }

    public static <T> SeqGroupConsumer<T> toDrainingSeqGroupConsumer(SeqGroupMapper<? super T, ?> seqGroupMapper) {
        Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");

        if (seqGroupMapper == SeqGroupMapper.identity()) {
            return drainingSeqGroupConsumer();
        }

        return (cancelToken, producer) -> {
            seqGroupMapper.mapAll(cancelToken, producer, drainingSeqGroupConsumer());
        };
    }

    public static <T> SeqGroupConsumer<T> backgroundRetainedSequencesSeqGroupConsumer(
            SeqGroupConsumer<? super T> seqGroupConsumer,
            String executorName,
            int queueSize
    ) {
        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");
        Objects.requireNonNull(executorName, "executorName");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqGroupProducer) -> {
            SeqGroupProducer<? extends T> backgroundProducer = seqGroupProducer
                    .toFluent()
                    .toBackgroundRetainSequences(executorName, queueSize)
                    .unwrap();
            seqGroupConsumer.consumeAll(cancelToken, backgroundProducer);
        };
    }

    public static <T> SeqGroupConsumer<T> backgroundRetainedSequencesSeqGroupConsumer(
            SeqGroupConsumer<? super T> seqGroupConsumer,
            ThreadFactory threadFactory,
            int queueSize
    ) {
        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");
        Objects.requireNonNull(threadFactory, "threadFactory");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqGroupProducer) -> {
            SeqGroupProducer<? extends T> backgroundProducer = seqGroupProducer
                    .toFluent()
                    .toBackgroundRetainSequences(threadFactory, queueSize)
                    .unwrap();
            seqGroupConsumer.consumeAll(cancelToken, backgroundProducer);
        };
    }

    public static <T> SeqGroupConsumer<T> backgroundRetainedSequencesSeqGroupConsumer(
            SeqGroupConsumer<? super T> seqGroupConsumer,
            TaskExecutor executor,
            int queueSize
    ) {
        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");
        Objects.requireNonNull(executor, "executor");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqGroupProducer) -> {
            SeqGroupProducer<? extends T> backgroundProducer = seqGroupProducer
                    .toFluent()
                    .toBackgroundRetainSequences(executor, queueSize)
                    .unwrap();
            seqGroupConsumer.consumeAll(cancelToken, backgroundProducer);
        };
    }

    public static <T> SeqConsumer<T> backgroundSeqConsumer(
            SeqConsumer<? super T> seqConsumer,
            String executorName,
            int queueSize
    ) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");
        Objects.requireNonNull(executorName, "executorName");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqProducer) -> {
            SeqProducer<? extends T> backgroundProducer = seqProducer
                    .toFluent()
                    .toBackground(executorName, queueSize)
                    .unwrap();
            seqConsumer.consumeAll(cancelToken, backgroundProducer);
        };
    }

    public static <T> SeqConsumer<T> backgroundSeqConsumer(
            SeqConsumer<? super T> seqConsumer,
            ThreadFactory threadFactory,
            int queueSize
    ) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");
        Objects.requireNonNull(threadFactory, "threadFactory");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqProducer) -> {
            SeqProducer<? extends T> backgroundProducer = seqProducer
                    .toFluent()
                    .toBackground(threadFactory, queueSize)
                    .unwrap();
            seqConsumer.consumeAll(cancelToken, backgroundProducer);
        };
    }

    public static <T> SeqConsumer<T> backgroundSeqConsumer(
            SeqConsumer<? super T> seqConsumer,
            TaskExecutor executor,
            int queueSize
    ) {
        Objects.requireNonNull(seqConsumer, "seqConsumer");
        Objects.requireNonNull(executor, "executor");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqProducer) -> {
            SeqProducer<? extends T> backgroundProducer = seqProducer
                    .toFluent()
                    .toBackground(executor, queueSize)
                    .unwrap();
            seqConsumer.consumeAll(cancelToken, backgroundProducer);
        };
    }

    public static <T, R> ElementConsumer<T> mapToConsumer(
            ElementMapper<? super T, ? extends R> mapper,
            ElementConsumer<? super R> consumer) {

        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(consumer, "consumer");

        if (mapper == ElementMappers.identityMapper()) {
            // T == R
            @SuppressWarnings("unchecked")
            ElementConsumer<? super T> result = (ElementConsumer<? super T>) consumer;
            return castConsumer(result);
        }

        return element -> mapper.map(element, consumer);
    }

    public static <T, R> SeqConsumer<T> mapToSeqConsumer(
            SeqMapper<? super T, ? extends R> seqMapper,
            SeqConsumer<? super R> seqConsumer) {

        Objects.requireNonNull(seqMapper, "seqMapper");
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        if (seqMapper == ElementMappers.identitySeqMapper()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqConsumer<? super T> seqConsumerCast = (SeqConsumer<? super T>) seqConsumer;
            return castSeqConsumer(seqConsumerCast);
        }

        return (cancelToken, seqProducer) -> {
            seqMapper.mapAll(cancelToken, seqProducer, seqConsumer);
        };
    }

    public static <T, R> SeqGroupConsumer<T> mapToSeqGroupConsumer(
            SeqGroupMapper<? super T, ? extends R> seqGroupMapper,
            SeqGroupConsumer<? super R> seqGroupConsumer) {

        Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");
        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");

        if (seqGroupMapper == ElementMappers.identitySeqGroupMapper()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqGroupConsumer<? super T> seqGroupConsumerCast = (SeqGroupConsumer<? super T>) seqGroupConsumer;
            return castSeqGroupConsumer(seqGroupConsumerCast);
        }

        return (cancelToken, seqGroupProducer) -> {
            seqGroupMapper.mapAll(cancelToken, seqGroupProducer, seqGroupConsumer);
        };
    }

    public static <T, C extends Iterable<? extends T>> SeqGroupConsumer<C> flatteningSeqGroupConsumer(
            SeqGroupConsumer<? super T> seqGroupConsumer) {

        Objects.requireNonNull(seqGroupConsumer, "seqGroupConsumer");

        if (seqGroupConsumer == drainingSeqGroupConsumer()) {
            return drainingSeqGroupConsumer();
        }

        return (cancelToken, seqGroupProducer) -> {
            seqGroupConsumer.consumeAll(cancelToken, ElementProducers.flatteningSeqGroupProducer(seqGroupProducer));
        };
    }

    public static <T, C extends Iterable<? extends T>> SeqConsumer<C> flatteningSeqConsumer(
            SeqConsumer<? super T> destSeqConsumer) {

        Objects.requireNonNull(destSeqConsumer, "estSeqConsumer");

        if (destSeqConsumer == drainingSeqConsumer()) {
            return drainingSeqConsumer();
        }

        return (cancelToken, seqProducer) -> {
            destSeqConsumer.consumeAll(cancelToken, ElementProducers.flatteningSeqProducer(seqProducer));
        };
    }

    public static <T, C extends Iterable<? extends T>> ElementConsumer<C> flatteningConsumer(
            ElementConsumer<? super T> elementConsumer) {

        Objects.requireNonNull(elementConsumer, "elementConsumer");

        if (elementConsumer == noOpConsumer()) {
            return noOpConsumer();
        }

        return elements -> {
            try {
                elements.forEach(element -> {
                    try {
                        elementConsumer.processElement(element);
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new WrapperException(ex);
                    }
                });
            } catch (WrapperException ex) {
                throw ExceptionHelper.throwChecked(ex.getCause(), Exception.class);
            }
        };
    }

    private static class WrapperException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public WrapperException(Throwable cause) {
            super("", cause, false, false);
        }
    }

    private ElementConsumers() {
        throw new AssertionError();
    }
}
