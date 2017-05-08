package org.jtrim2.concurrent.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class CollectListener<DataType> implements AsyncDataListener<DataType> {
    private final Runnable onDoneCheck;

    private final Queue<DataType> results;
    private final AtomicReference<AsyncReport> reportRef;
    private final WaitableSignal doneSignal;
    private final AtomicReference<String> miscErrorRef;

    public CollectListener() {
        this(Tasks.noOpTask());
    }

    public CollectListener(Runnable onDoneCheck) {
        Objects.requireNonNull(onDoneCheck, "onDoneCheck");

        this.onDoneCheck = onDoneCheck;
        this.results = new ConcurrentLinkedQueue<>();
        this.reportRef = new AtomicReference<>(null);
        this.doneSignal = new WaitableSignal();
        this.miscErrorRef = new AtomicReference<>(null);
    }

    private <T> int findInList(T toFind, List<T> list, int startOffset) {
        int size = list.size();
        for (int i = startOffset; i < size; i++) {
            if (list.get(i) == toFind) {
                return i;
            }
        }
        return -1;
    }

    private <ResultType> void checkValidResults(ResultType[] expectedResults, List<ResultType> actualResults) {
        int expectedIndex = 0;
        for (ResultType actual: actualResults) {
            int foundIndex = findInList(actual, actualResults, expectedIndex);
            if (foundIndex < 0) {
                fail("Unexpected results: " + actualResults
                        + " (expected = " + Arrays.toString(expectedResults) + ")");
            }
        }
    }

    public <ResultType> List<ResultType> extractedResults(DataConverter<DataType, ResultType> extractor) {
        List<DataType> currentResults = getResults();
        List<ResultType> extracted = new ArrayList<>(currentResults.size());

        for (DataType result: currentResults) {
            extracted.add(extractor.convertData(result));
        }
        return extracted;
    }

    public <ResultType> void checkValidResults(
            ResultType[] expectedResults,
            DataConverter<DataType, ResultType> extractor) {
        checkValidResults(expectedResults, extractedResults(extractor));
    }

    public void checkValidResults(DataType[] expectedResults) {
        checkValidResults(expectedResults, getResults());
    }

    private <ResultType> void checkValidCompleteResults(ResultType[] expectedResults, List<ResultType> actualResults) {
        if (expectedResults.length == 0) {
            assertEquals("Expected no results.", 0, actualResults.size());
            return;
        }

        if (actualResults.isEmpty()) {
            fail("Need at least one result.");
        }

        assertEquals(
                "The final result must match the final expected result.",
                expectedResults[expectedResults.length - 1],
                actualResults.get(actualResults.size() - 1));

        checkValidResults(expectedResults, actualResults);
    }

    public <ResultType> void checkValidCompleteResults(
            ResultType[] expectedResults,
            DataConverter<DataType, ResultType> extractor) {
        checkValidCompleteResults(expectedResults, extractedResults(extractor));
    }

    public void checkValidCompleteResults(DataType[] expectedResults) {
        checkValidCompleteResults(expectedResults, getResults());
    }

    private void setMiscError(String error) {
        miscErrorRef.compareAndSet(null, error);
    }

    public String getMiscError() {
        return miscErrorRef.get();
    }

    public boolean isCompleted() {
        return doneSignal.isSignaled();
    }

    public boolean tryWaitCompletion(long timeout, TimeUnit unit) {
        return doneSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    public List<DataType> getResults() {
        return new ArrayList<>(results);
    }

    public AsyncReport getReport() {
        return reportRef.get();
    }

    @Override
    public void onDataArrive(DataType data) {
        results.add(data);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        try {
            try {
                onDoneCheck.run();
            } catch (Throwable checkError) {
                setMiscError(checkError.getMessage());
            }
            if (!reportRef.compareAndSet(null, report)) {
                setMiscError("Report has been sent multiple times.");
            }
        } finally {
            doneSignal.signal();
        }
    }
}
