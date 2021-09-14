package org.jtrim2.jobprocessing;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ArraysEx;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class ElementProducers {
    public static <T> SeqProducer<T> emptySeqProducer() {
        return (cancelToken, consumer) -> { };
    }

    public static <T> SeqGroupProducer<T> emptySeqGroupProducer() {
        return (cancelToken, seqConsumer) -> { };
    }

    private static <T> SeqProducer<T> castSeqProducer(SeqProducer<? extends T> seqProducer) {
        return seqProducer::transferAll;
    }

    private static <T> SeqGroupProducer<T> castSeqGroupProducer(SeqGroupProducer<? extends T> seqGroupProducer) {
        return seqGroupProducer::transferAll;
    }

    public static <T> SeqProducer<T> concat(SeqProducer<? extends T> producer1, SeqProducer<? extends T> producer2) {
        Objects.requireNonNull(producer1, "producer1");
        Objects.requireNonNull(producer2, "producer2");

        return (CancellationToken cancelToken, ElementConsumer<? super T> consumer) -> {
            producer1.transferAll(cancelToken, consumer);
            producer2.transferAll(cancelToken, consumer);
        };
    }

    public static <T> SeqProducer<T> peekedSeqProducerContextFree(
            SeqProducer<? extends T> seqProducer,
            ElementConsumer<? super T> peeker) {

        Objects.requireNonNull(seqProducer, "seqProducer");
        Objects.requireNonNull(peeker, "peeker");

        if (seqProducer == emptySeqProducer()) {
            // There is nothign to peek anyway.
            return emptySeqProducer();
        }
        if (peeker == ElementConsumer.noOp()) {
            return castSeqProducer(seqProducer);
        }

        return (CancellationToken cancelToken, ElementConsumer<? super T> consumer) -> {
            seqProducer.transferAll(cancelToken, ElementConsumers.concatConsumers(peeker, consumer));
        };
    }

    public static <T> SeqProducer<T> postPeekedSeqProducerContextFree(
            SeqProducer<? extends T> seqProducer,
            ElementConsumer<? super T> peeker) {

        Objects.requireNonNull(seqProducer, "seqProducer");
        Objects.requireNonNull(peeker, "peeker");

        if (seqProducer == emptySeqProducer()) {
            // There is nothign to peek anyway.
            return emptySeqProducer();
        }
        if (peeker == ElementConsumer.noOp()) {
            return castSeqProducer(seqProducer);
        }

        return (CancellationToken cancelToken, ElementConsumer<? super T> consumer) -> {
            seqProducer.transferAll(cancelToken, ElementConsumers.concatConsumers(consumer, peeker));
        };
    }

    public static <T> SeqProducer<T> peekedSeqProducer(
            SeqProducer<? extends T> seqProducer,
            SeqConsumer<? super T> seqPeeker) {

        Objects.requireNonNull(seqProducer, "seqProducer");
        Objects.requireNonNull(seqPeeker, "seqPeeker");

        if (seqProducer == emptySeqProducer()) {
            // There is nothign to peek anyway.
            return emptySeqProducer();
        }
        if (seqPeeker == SeqConsumer.draining()) {
            return castSeqProducer(seqProducer);
        }

        return (cancelToken, consumer) -> {
            seqPeeker.consumeAll(cancelToken, postPeekedSeqProducerContextFree(seqProducer, consumer));
        };
    }

    public static <T> SeqGroupProducer<T> peekedSeqGroupProducerContextFree(
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqConsumer<? super T> seqPeeker) {

        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");
        Objects.requireNonNull(seqPeeker, "seqPeeker");

        if (seqGroupProducer == emptySeqGroupProducer()) {
            // There is nothign to peek anyway.
            return emptySeqGroupProducer();
        }
        if (seqPeeker == SeqConsumer.draining()) {
            return castSeqGroupProducer(seqGroupProducer);
        }

        return (cancelToken, seqConsumer) -> {
            seqGroupProducer.transferAll(cancelToken, ElementConsumers.concatSeqConsumers(seqPeeker, seqConsumer));
        };
    }

    public static <T> SeqGroupProducer<T> postPeekedSeqGroupProducer(
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqConsumer<? super T> seqPeeker) {

        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");
        Objects.requireNonNull(seqPeeker, "seqPeeker");

        if (seqGroupProducer == emptySeqGroupProducer()) {
            // There is nothign to peek anyway.
            return emptySeqGroupProducer();
        }
        if (seqPeeker == SeqConsumer.draining()) {
            return castSeqGroupProducer(seqGroupProducer);
        }

        return (cancelToken, seqConsumer) -> {
            seqGroupProducer.transferAll(cancelToken, ElementConsumers.concatSeqConsumers(seqConsumer, seqPeeker));
        };
    }

    public static <T> SeqGroupProducer<T> peekedSeqGroupProducer(
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqGroupConsumer<? super T> seqGroupPeeker) {

        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");
        Objects.requireNonNull(seqGroupPeeker, "seqGroupPeeker");

        if (seqGroupProducer == emptySeqGroupProducer()) {
            // There is nothign to peek anyway.
            return emptySeqGroupProducer();
        }
        if (seqGroupPeeker == SeqGroupConsumer.draining()) {
            return castSeqGroupProducer(seqGroupProducer);
        }

        return (cancelToken, seqConsumer) -> {
            seqGroupPeeker.consumeAll(cancelToken, postPeekedSeqGroupProducer(seqGroupProducer, seqConsumer));
        };
    }

    public static <T, R> SeqProducer<R> mapSeqProducer(
            SeqProducer<? extends T> seqProducer,
            SeqMapper<? super T, ? extends R> seqMapper) {

        Objects.requireNonNull(seqProducer, "seqProducer");
        Objects.requireNonNull(seqMapper, "seqMapper");

        if (seqProducer == emptySeqProducer()) {
            // Nothing to map
            return emptySeqProducer();
        }
        if (seqMapper == SeqMapper.identity()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqProducer<? extends R> source = (SeqProducer<? extends R>) seqProducer;
            return castSeqProducer(source);
        }

        return (cancelToken, consumer) -> {
            SeqConsumer<R> seqConsumer = ElementConsumers.contextFreeSeqConsumer(consumer);
            seqMapper.mapAll(cancelToken, seqProducer, seqConsumer);
        };
    }

    public static <T, R> SeqGroupProducer<R> contextFreeMapSeqGroupProducer(
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqMapper<? super T, ? extends R> seqMapper) {

        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");
        Objects.requireNonNull(seqMapper, "seqMapper");

        if (seqGroupProducer == emptySeqGroupProducer()) {
            // Nothing to map
            return emptySeqGroupProducer();
        }
        if (seqMapper == SeqMapper.identity()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqGroupProducer<? extends R> source = (SeqGroupProducer<? extends R>) seqGroupProducer;
            return castSeqGroupProducer(source);
        }

        return (cancelToken, seqConsumer) -> {
            SeqConsumer<T> mappedConsumer = ElementConsumers.mapToSeqConsumer(seqMapper, seqConsumer);
            seqGroupProducer.transferAll(cancelToken, mappedConsumer);
        };
    }

    public static <T, R> SeqGroupProducer<R> mapSeqGroupProducer(
            SeqGroupProducer<? extends T> seqGroupProducer,
            SeqGroupMapper<? super T, ? extends R> seqGroupMapper) {

        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");
        Objects.requireNonNull(seqGroupMapper, "seqGroupMapper");

        if (seqGroupProducer == emptySeqGroupProducer()) {
            // Nothing to map
            return emptySeqGroupProducer();
        }
        if (seqGroupMapper == SeqGroupMapper.identity()) {
            // T == R
            @SuppressWarnings("unchecked")
            SeqGroupProducer<? extends R> source = (SeqGroupProducer<? extends R>) seqGroupProducer;
            return castSeqGroupProducer(source);
        }

        return (cancelToken, seqConsumer) -> {
            SeqGroupConsumer<R> seqGroupConsumer = ElementConsumers.contextFreeSeqGroupConsumer(seqConsumer);
            seqGroupMapper.mapAll(cancelToken, seqGroupProducer, seqGroupConsumer);
        };
    }

    public static <T> SeqGroupProducer<T> toSingleGroupProducer(SeqProducer<? extends T> seqProducer) {
        Objects.requireNonNull(seqProducer, "seqProducer");

        return (cancelToken, seqConsumer) -> seqConsumer.consumeAll(cancelToken, seqProducer);
    }

    public static <T> SeqProducer<T> toSynchronizedSeqProducer(SeqGroupProducer<? extends T> seqGroupProducer) {
        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");

        return (cancelToken, consumer) -> {
            seqGroupProducer.transferAllSimple(cancelToken, ElementConsumers.toSynchronizedConsumer(consumer));
        };
    }

    public static <T> SeqGroupProducer<List<T>> batchProducer(
            int batchSizePerGroup,
            SeqGroupProducer<? extends T> seqGroupProducer) {

        ExceptionHelper.checkArgumentInRange(batchSizePerGroup, 1, Integer.MAX_VALUE, "batchSizePerGroup");
        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");

        return (cancelToken, seqConsumer) -> {
            seqGroupProducer.transferAll(cancelToken, (consumerCancelToken, seqProducer) -> {
                seqConsumer.consumeAll(consumerCancelToken, batchSerialProducer(batchSizePerGroup, seqProducer));
            });
        };
    }

    public static <T> SeqProducer<List<T>> batchSerialProducer(
            int batchSize,
            SeqProducer<? extends T> seqProducer) {

        ExceptionHelper.checkArgumentInRange(batchSize, 1, Integer.MAX_VALUE, "batchSize");
        Objects.requireNonNull(seqProducer, "seqProducer");

        if (seqProducer == emptySeqProducer()) {
            return emptySeqProducer();
        }

        return (cancelToken, consumer) -> {
            @SuppressWarnings("unchecked")
            T[] batch = (T[]) new Object[batchSize];
            int[] indexRef = new int[1];

            seqProducer.transferAll(cancelToken, element -> {
                int index = indexRef[0];
                batch[index] = Objects.requireNonNull(element, "element");
                index++;

                if (index >= batchSize) {
                    index = 0;
                    List<T> batchCopy = ArraysEx.viewAsList(batch.clone());
                    Arrays.fill(batch, null);

                    consumer.processElement(batchCopy);
                }

                indexRef[0] = index;
            });

            int count = indexRef[0];
            if (count > 0) {
                consumer.processElement(ArraysEx.viewAsList(batch, 0, count));
            }
        };
    }

    public static <T> SeqProducer<T> flatteningSeqProducer(
            SeqProducer<? extends Iterable<? extends T>> srcSeqProducer) {

        Objects.requireNonNull(srcSeqProducer, "srcSeqProducer");

        if (srcSeqProducer == ElementProducers.<Iterable<T>>emptySeqProducer()) {
            return emptySeqProducer();
        }

        return (cancelToken, consumer) -> {
            srcSeqProducer.transferAll(cancelToken, ElementConsumers.flatteningConsumer(consumer));
        };
    }

    public static <T> SeqGroupProducer<T> flatteningSeqGroupProducer(
            SeqGroupProducer<? extends Iterable<? extends T>> srcSeqGroupProducer) {

        Objects.requireNonNull(srcSeqGroupProducer, "srcSeqGroupProducer");

        if (srcSeqGroupProducer == ElementProducers.<Iterable<T>>emptySeqGroupProducer()) {
            return emptySeqGroupProducer();
        }

        return (cancelToken, destSeqConsumer) -> {
            srcSeqGroupProducer.transferAll(cancelToken, ElementConsumers.flatteningSeqConsumer(destSeqConsumer));
        };
    }

    public static <T> SeqGroupProducer<T> backgroundSeqGroupProducer(
            String executorName,
            int consumerThreadCount,
            int queueSize,
            SeqGroupProducer<? extends T> seqGroupProducer) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.owned(executorName, consumerThreadCount);
        return new ParallelSeqGroupProducer<>(executorRefProvider, consumerThreadCount, queueSize, seqGroupProducer);
    }

    public static <T> SeqGroupProducer<T> backgroundSeqGroupProducer(
            TaskExecutor executor,
            int consumerThreadCount,
            int queueSize,
            SeqGroupProducer<? extends T> seqGroupProducer) {

        Supplier<ExecutorRef> executorRefProvider = ExecutorRef.external(executor);
        return new ParallelSeqGroupProducer<>(executorRefProvider, consumerThreadCount, queueSize, seqGroupProducer);
    }

    private static <T, R, A> SeqConsumer<T> accumulatorSeqConsumer(
            AtomicReference<A> totalAccRef,
            Collector<? super T, A, ? extends R> collector) {

        return (cancelToken, seqProducer) -> {
            A currentAcc = collectSerialAcc(cancelToken, seqProducer, collector);
            updateAccumulator(collector, totalAccRef, currentAcc);
        };
    }

    public static <T, R, A> R collect(
            CancellationToken cancelToken,
            SeqGroupProducer<? extends T> seqGroupProducer,
            Collector<? super T, A, ? extends R> collector) throws Exception {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(seqGroupProducer, "seqGroupProducer");
        Objects.requireNonNull(collector, "collector");

        AtomicReference<A> totalAccRef = new AtomicReference<>(null);
        seqGroupProducer.transferAll(cancelToken, accumulatorSeqConsumer(totalAccRef, collector));

        A totalAcc = totalAccRef.get();
        if (totalAcc == null) {
            totalAcc = collector.supplier().get();
        }

        return collector.finisher().apply(totalAcc);
    }

    public static <T, R, A> R collectSerial(
            CancellationToken cancelToken,
            SeqProducer<? extends T> serialProducer,
            Collector<? super T, A, ? extends R> collector) throws Exception {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(serialProducer, "serialProducer");
        Objects.requireNonNull(collector, "collector");

        A acc = collectSerialAcc(cancelToken, serialProducer, collector);
        if (acc == null) {
            acc = collector.supplier().get();
        }
        return collector.finisher().apply(acc);
    }

    private static <T, R, A> A collectSerialAcc(
            CancellationToken cancelToken,
            SeqProducer<? extends T> serialProducer,
            Collector<? super T, A, ? extends R> collector) throws Exception {

        A resultContainer = collector.supplier().get();
        BiConsumer<A, ? super T> accumulator = collector.accumulator();

        serialProducer.transferAll(cancelToken, element -> {
            accumulator.accept(resultContainer, element);
        });

        return resultContainer;
    }

    private static <A> void updateAccumulator(
            Collector<?, A, ? > collector,
            AtomicReference<A> totalAccRef,
            A partialAcc) {

        if (partialAcc == null) {
            return;
        }

        A newTotalAcc = partialAcc;
        do {
            A currentTotalAcc = totalAccRef.getAndSet(null);
            if (currentTotalAcc != null) {
                newTotalAcc = collector.combiner().apply(currentTotalAcc, newTotalAcc);
            }
        } while (!totalAccRef.compareAndSet(null, newTotalAcc));
    }

    private ElementProducers() {
        throw new AssertionError();
    }
}
