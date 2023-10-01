package org.jtrim2.stream;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class ElementMappers {
    public static <T> ElementMapper<T, T> identityMapper() {
        return (element, consumer) -> {
            consumer.processElement(element);
        };
    }

    public static <T> SeqMapper<T, T> identitySeqMapper() {
        return (cancelToken, seqProducer, seqConsumer) -> seqConsumer.consumeAll(cancelToken, seqProducer);
    }

    public static <T> SeqGroupMapper<T, T> identitySeqGroupMapper() {
        return (cancelToken, seqGroupPrdocuer, seqGroupProducer) -> {
            seqGroupProducer.consumeAll(cancelToken, seqGroupPrdocuer);
        };
    }

    private static <T, R> SeqMapper<T, R> castCustomSeqMapper(SeqMapper<? super T, ? extends R> seqMapper) {
        return seqMapper::mapAll;
    }

    @SuppressWarnings("unchecked")
    private static <T, R> SeqMapper<T, R> castSeqMapper(SeqMapper<? super T, ? extends R> seqMapper) {
        if (seqMapper == identitySeqMapper()) {
            // Even though the wild cards can be anything, an identity mapper is just passing around
            // the same object, so there can't be any type safety issue.
            // It wouldn't be necessarily safe in general, because `seqMapper` could have additional
            // methods using its generic arguments contradictionally to (? super T) or (? extends R).
            return (SeqMapper<T, R>) seqMapper;
        } else {
            return castCustomSeqMapper(seqMapper);
        }
    }

    private static <T, R> SeqGroupMapper<T, R> castCustomSeqGroupMapper(
            SeqGroupMapper<? super T, ? extends R> seqGroupMapper) {

        return seqGroupMapper::mapAll;
    }

    @SuppressWarnings("unchecked")
    private static <T, R> SeqGroupMapper<T, R> castSeqGroupMapper(
            SeqGroupMapper<? super T, ? extends R> seqGroupMapper) {

        if (seqGroupMapper == identitySeqGroupMapper()) {
            // Even though the wild cards can be anything, an identity mapper is just passing around
            // the same object, so there can't be any type safety issue.
            // It wouldn't be necessarily safe in general, because `seqGroupMapper` could have additional
            // methods using its generic arguments contradictionally to (? super T) or (? extends R).
            return (SeqGroupMapper<T, R>) seqGroupMapper;
        } else {
            return castCustomSeqGroupMapper(seqGroupMapper);
        }
    }

    public static <T, R> SeqMapper<T, R> contextFreeSeqMapper(
            ElementMapper<? super T, ? extends R> mapper) {

        Objects.requireNonNull(mapper, "mapper");

        if (mapper == identityMapper()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqMapper<T, R> result = (SeqMapper<T, R>) identitySeqMapper();
            return result;
        }

        return (cancelToken, seqProducer, seqConsumer) -> {
            seqConsumer.consumeAll(cancelToken, ElementProducers.mapSeqProducerContextFree(seqProducer, mapper));
        };
    }

    public static <T, R> SeqGroupMapper<T, R> contextFreeSeqGroupMapper(
            SeqMapper<? super T, ? extends R> seqMapper) {

        Objects.requireNonNull(seqMapper, "seqMapper");

        if (seqMapper == identitySeqMapper()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqGroupMapper<T, R> result = (SeqGroupMapper<T, R>) identitySeqGroupMapper();
            return result;
        }

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            seqGroupConsumer.consumeAll(cancelToken, (producerCancelToken, seqConsumer) -> {
                SeqConsumer<T> mappingSeqConsumer = ElementConsumers.mapToSeqConsumer(seqMapper, seqConsumer);
                seqGroupProducer.transferAll(producerCancelToken, mappingSeqConsumer);
            });
        };
    }

    public static <T, R> SeqGroupMapper<T, R> contextFreeSeqGroupMapper(ElementMapper<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");

        if (mapper == identityMapper()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqGroupMapper<T, R> result = (SeqGroupMapper<T, R>) identitySeqGroupMapper();
            return result;
        }

        return contextFreeSeqGroupMapper((cancelToken, seqProducer, seqMapper) -> {
            seqMapper.consumeAll(cancelToken, (producerCancelToken, consumer) -> {
                seqProducer.transferAll(producerCancelToken, element -> {
                    mapper.map(element, consumer);
                });
            });
        });
    }


    private static <T, R> SeqMapper<T, R> toSingleShotSeqMapper(
            SeqMapper<? super T, ? extends R> seqMapper) {

        Objects.requireNonNull(seqMapper, "seqMapper");

        AtomicBoolean started = new AtomicBoolean(false);
        return (cancelToken, seqProducer, seqConsumer) -> {
            if (!started.compareAndSet(false, true)) {
                throw new IllegalStateException("This mapper cannot process multiple groups.");
            }
            seqMapper.mapAll(cancelToken, seqProducer, seqConsumer);
        };
    }

    public static <T, R> SeqGroupMapper<T, R> toSingleShotSeqGroupMapper(
            SeqMapper<? super T, ? extends R> seqMapper) {

        Objects.requireNonNull(seqMapper, "seqMapper");

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            SeqGroupMapper<T, R> singleShotMapper = contextFreeSeqGroupMapper(toSingleShotSeqMapper(seqMapper));
            singleShotMapper.mapAll(cancelToken, seqGroupProducer, seqGroupConsumer);
        };
    }

    public static <T1, T2, T3> SeqMapper<T1, T3> concatSeqMapper(
            SeqMapper<? super T1, ? extends T2> seqMapper1,
            SeqMapper<? super T2, ? extends T3> seqMapper2) {

        Objects.requireNonNull(seqMapper1, "seqMapper1");
        Objects.requireNonNull(seqMapper2, "seqMapper2");

        if (seqMapper1 == identitySeqMapper()) {
            // T1 == T2
            @SuppressWarnings("unchecked")
            SeqMapper<? super T1, ? extends T3> result
                    = (SeqMapper<? super T1, ? extends T3>) seqMapper2;
            return castSeqMapper(result);
        }
        if (seqMapper2 == identitySeqMapper()) {
            // T2 == T3
            @SuppressWarnings("unchecked")
            SeqMapper<? super T1, ? extends T3> result
                    = (SeqMapper<? super T1, ? extends T3>) seqMapper1;
            return castSeqMapper(result);
        }

        return (cancelToken, seqProducer, seqConsumer) -> {
            SeqConsumer<T2> mappedSeqGroupConsumer = ElementConsumers
                    .mapToSeqConsumer(seqMapper2, seqConsumer);
            seqMapper1.mapAll(cancelToken, seqProducer, mappedSeqGroupConsumer);
        };
    }

    public static <T1, T2, T3> SeqGroupMapper<T1, T3> concatSeqGroupMapper(
            SeqGroupMapper<? super T1, ? extends T2> seqGroupMapper1,
            SeqGroupMapper<? super T2, ? extends T3> seqGroupMapper2) {

        Objects.requireNonNull(seqGroupMapper1, "seqGroupMapper1");
        Objects.requireNonNull(seqGroupMapper2, "seqGroupMapper2");

        if (seqGroupMapper1 == identitySeqGroupMapper()) {
            // T1 == T2
            @SuppressWarnings("unchecked")
            SeqGroupMapper<? super T1, ? extends T3> result
                    = (SeqGroupMapper<? super T1, ? extends T3>) seqGroupMapper2;
            return castSeqGroupMapper(result);
        }
        if (seqGroupMapper2 == identitySeqGroupMapper()) {
            // T2 == T3
            @SuppressWarnings("unchecked")
            SeqGroupMapper<? super T1, ? extends T3> result
                    = (SeqGroupMapper<? super T1, ? extends T3>) seqGroupMapper1;
            return castSeqGroupMapper(result);
        }

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            SeqGroupConsumer<T2> mappedSeqGroupConsumer = ElementConsumers
                    .mapToSeqGroupConsumer(seqGroupMapper2, seqGroupConsumer);
            seqGroupMapper1.mapAll(cancelToken, seqGroupProducer, mappedSeqGroupConsumer);
        };
    }

    public static <T> SeqGroupMapper<T, T> toInspectorMapper(SeqGroupConsumer<? super T> seqGroupInspector) {
        Objects.requireNonNull(seqGroupInspector, "seqGroupInspector");

        if (seqGroupInspector == SeqGroupConsumer.draining()) {
            return ElementMappers.identitySeqGroupMapper();
        }

        return (cancelToken, seqGroupProducer, consumerDef) -> {
            SeqGroupProducer<T> peekedSeqGroupProducer
                    = ElementProducers.peekedSeqGroupProducer(seqGroupProducer, seqGroupInspector);
            consumerDef.consumeAll(cancelToken, peekedSeqGroupProducer);
        };
    }

    public static <T, C extends Iterable<? extends T>> ElementMapper<C, T> flatteningMapper() {
        return (element, consumer) -> {
            ElementConsumers
                    .flatteningConsumer(consumer)
                    .processElement(element);
        };
    }

    public static <T, C extends Iterable<? extends T>> SeqMapper<C, T> flatteningSeqMapper() {
        return (cancelToken, seqProducer, seqConsumer) -> {
            SeqProducer<T> flatSeqGroupProducer = ElementProducers.flatteningSeqProducer(seqProducer);
            seqConsumer.consumeAll(cancelToken, flatSeqGroupProducer);
        };
    }

    public static <T, C extends Iterable<? extends T>> SeqGroupMapper<C, T> flatteningSeqGroupMapper() {
        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            SeqGroupProducer<T> flatSeqGroupProducer = ElementProducers.flatteningSeqGroupProducer(seqGroupProducer);
            seqGroupConsumer.consumeAll(cancelToken, flatSeqGroupProducer);
        };
    }

    public static <T, R> SeqGroupMapper<T, R> inBackgroundRetainSequencesSeqGroupMapper(
            SeqGroupMapper<T, R> seqGroupMapper,
            String executorName,
            int queueSize
    ) {
        Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");
        Objects.requireNonNull(executorName, "executorName");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            SeqGroupProducer<? extends T> backgroundProducer = seqGroupProducer
                    .toFluent()
                    .toBackgroundRetainSequences(executorName, queueSize)
                    .unwrap();
            seqGroupMapper.mapAll(cancelToken, backgroundProducer, seqGroupConsumer);
        };
    }

    public static <T, R> SeqGroupMapper<T, R> inBackgroundRetainSequencesSeqGroupMapper(
            SeqGroupMapper<T, R> seqGroupMapper,
            ThreadFactory threadFactory,
            int queueSize
    ) {
        Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");
        Objects.requireNonNull(threadFactory, "threadFactory");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            SeqGroupProducer<? extends T> backgroundProducer = seqGroupProducer
                    .toFluent()
                    .toBackgroundRetainSequences(threadFactory, queueSize)
                    .unwrap();
            seqGroupMapper.mapAll(cancelToken, backgroundProducer, seqGroupConsumer);
        };
    }

    public static <T, R> SeqGroupMapper<T, R> inBackgroundRetainSequencesSeqGroupMapper(
            SeqGroupMapper<T, R> seqGroupMapper,
            TaskExecutor executor,
            int queueSize
    ) {
        Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");
        Objects.requireNonNull(executor, "executor");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            SeqGroupProducer<? extends T> backgroundProducer = seqGroupProducer
                    .toFluent()
                    .toBackgroundRetainSequences(executor, queueSize)
                    .unwrap();
            seqGroupMapper.mapAll(cancelToken, backgroundProducer, seqGroupConsumer);
        };
    }

    public static <T, R> SeqMapper<T, R> inBackgroundSeqMapper(
            SeqMapper<T, R> seqMapper,
            String executorName,
            int queueSize
    ) {
        Objects.requireNonNull(seqMapper, "seqMapper");
        Objects.requireNonNull(executorName, "executorName");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqProducer, seqConsumer) -> {
            SeqProducer<? extends T> backgroundProducer = seqProducer
                    .toFluent()
                    .toBackground(executorName, queueSize)
                    .unwrap();
            seqMapper.mapAll(cancelToken, backgroundProducer, seqConsumer);
        };
    }

    public static <T, R> SeqMapper<T, R> inBackgroundSeqMapper(
            SeqMapper<T, R> seqMapper,
            ThreadFactory threadFactory,
            int queueSize
    ) {
        Objects.requireNonNull(seqMapper, "seqMapper");
        Objects.requireNonNull(threadFactory, "threadFactory");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqProducer, seqConsumer) -> {
            SeqProducer<? extends T> backgroundProducer = seqProducer
                    .toFluent()
                    .toBackground(threadFactory, queueSize)
                    .unwrap();
            seqMapper.mapAll(cancelToken, backgroundProducer, seqConsumer);
        };
    }

    public static <T, R> SeqMapper<T, R> inBackgroundSeqMapper(
            SeqMapper<T, R> seqMapper,
            TaskExecutor executor,
            int queueSize
    ) {
        Objects.requireNonNull(seqMapper, "seqMapper");
        Objects.requireNonNull(executor, "executor");
        ExceptionHelper.checkArgumentInRange(queueSize, 0, Integer.MAX_VALUE, "queueSize");

        return (cancelToken, seqProducer, seqConsumer) -> {
            SeqProducer<? extends T> backgroundProducer = seqProducer
                    .toFluent()
                    .toBackground(executor, queueSize)
                    .unwrap();
            seqMapper.mapAll(cancelToken, backgroundProducer, seqConsumer);
        };
    }

    private ElementMappers() {
        throw new AssertionError();
    }
}
