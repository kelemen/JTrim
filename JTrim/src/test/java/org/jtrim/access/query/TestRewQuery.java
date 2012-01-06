/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.access.query;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.jtrim.concurrent.async.*;
import org.jtrim.utils.*;

/**
 *
 * @author Kelemen Attila
 */
public final class TestRewQuery implements RewQuery<String, String> {
    private static final String[] EMPTY_STR_ARRAY = new String[0];
    private static final AsyncDataState[] EMPTY_STATE_ARRAY = new AsyncDataState[0];

    private final Lock mainLock;
    private final String input;

    private final List<String> writtenOutput;
    private final List<AsyncDataState> writtenState;

    private volatile boolean canceled;

    private final int stepCount;
    private final int delayMS;

    private final ExecutorService executor;

    public TestRewQuery(ExecutorService executor, String input, int stepCount, int delayMS) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(input, "input");
        ExceptionHelper.checkArgumentInRange(stepCount, 0, Integer.MAX_VALUE, "stepCount");
        ExceptionHelper.checkArgumentInRange(delayMS, 0, Integer.MAX_VALUE, "delayMS");

        this.executor = executor;
        this.stepCount = stepCount;
        this.delayMS = delayMS;
        this.canceled = false;
        this.input = input;
        this.mainLock = new ReentrantLock();
        this.writtenOutput = new LinkedList<>();
        this.writtenState = new LinkedList<>();
    }

    public static String getExpectedResult(String arg, int index) {
        return arg + "." + index;
    }

    private <E> void addToList(List<? super E> list, E newElement) {
        mainLock.lock();
        try {
            list.add(newElement);
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public String readInput() {
        return input;
    }

    @Override
    public AsyncDataQuery<String, String> getOutputQuery() {
        return new AsyncDataQuery<String, String>() {
            @Override
            public AsyncDataLink<String> createDataLink(String arg) {
                return new TestLink(arg);
            }
        };
    }

    @Override
    public void writeOutput(String output) {
        addToList(writtenOutput, output);
    }

    @Override
    public void writeState(AsyncDataState state) {
        addToList(writtenState, state);
    }

    public String[] getWrittenOutput() {
        mainLock.lock();
        try {
            return writtenOutput.toArray(EMPTY_STR_ARRAY);
        } finally {
            mainLock.unlock();
        }
    }

    public AsyncDataState[] getWrittenState() {
        mainLock.lock();
        try {
            return writtenState.toArray(EMPTY_STATE_ARRAY);
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    private class TestTask implements Runnable {
        private final String arg;
        private final AtomicInteger currentState;
        private final AsyncDataListener<? super String> dataListener;

        private volatile boolean cancelWasNoticed;

        public TestTask(String arg, AsyncDataListener<? super String> dataListener) {
            ExceptionHelper.checkNotNullArgument(arg, "arg");
            this.arg = arg;
            this.currentState = new AtomicInteger(0);
            this.dataListener = dataListener;
            this.cancelWasNoticed = false;
        }

        @SuppressWarnings("SleepWhileInLoop")
        private void sendDatas() {
            currentState.set(0);
            for (int i = 0; i < stepCount; i++) {
                if (canceled) {
                    cancelWasNoticed = true;
                    break;
                }

                if (delayMS > 0) {
                    try {
                        Thread.sleep(delayMS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        cancelWasNoticed = true;
                        break;
                    }
                }

                dataListener.onDataArrive(getExpectedResult(arg, i));
                currentState.set(i + 1);
            }
        }

        @Override
        public void run() {
            Throwable error = null;
            try {
                sendDatas();
            } catch (Throwable ex) {
                error = ex;
            } finally {
                dataListener.onDoneReceive(AsyncReport.getReport(error, cancelWasNoticed));
            }
        }

        public double getCurrentProgress() {
            assert stepCount >= 0;
            int state = currentState.get();
            if (state >= stepCount) {
                return 1.0;
            }
            else {
                return (double)state / (double)stepCount;
            }
        }
    }

    private class TestLink implements AsyncDataLink<String> {
        private final String arg;

        public TestLink(String arg) {
            ExceptionHelper.checkNotNullArgument(arg, "arg");
            this.arg = arg;
        }

        @Override
        public AsyncDataController getData(AsyncDataListener<? super String> dataListener) {
            ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

            final TestTask task = new TestTask(arg, dataListener);
            final Future<?> future = executor.submit(task);
            return new AsyncDataController() {
                @Override
                public void controlData(Object controlArg) {
                }

                @Override
                public void cancel() {
                    future.cancel(true);
                }

                @Override
                public AsyncDataState getDataState() {
                    return new SimpleDataState("Test State", task.getCurrentProgress());
                }
            };
        }
    }
}
