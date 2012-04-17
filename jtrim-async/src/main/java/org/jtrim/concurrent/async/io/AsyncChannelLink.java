package org.jtrim.concurrent.async.io;

import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.concurrent.async.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class AsyncChannelLink<DataType> implements AsyncDataLink<DataType> {
    private static final Logger LOGGER = Logger.getLogger(AsyncChannelLink.class.getName());

    private final CheckedAsyncChannelLink<DataType, ?> impl;

    public <ChannelType extends Channel> AsyncChannelLink(
            ExecutorService executor,
            ExecutorService cancelExecutor,
            ChannelOpener<? extends ChannelType> channelOpener,
            ChannelProcessor<DataType, ChannelType> channelProcessor) {

        this.impl = new CheckedAsyncChannelLink<>(
                executor,
                cancelExecutor,
                channelOpener,
                channelProcessor);
    }

    @Override
    public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener) {
        return impl.getData(dataListener);
    }

    private static class CheckedAsyncChannelLink<DataType, ChannelType extends Channel> {
        private final ExecutorService executor;
        private final ExecutorService cancelExecutor;
        private final ChannelOpener<? extends ChannelType> channelOpener;
        private final ChannelProcessor<DataType, ChannelType> channelProcessor;

        public CheckedAsyncChannelLink(
                ExecutorService executor,
                ExecutorService cancelExecutor,
                ChannelOpener<? extends ChannelType> channelOpener,
                ChannelProcessor<DataType, ChannelType> channelProcessor) {

            ExceptionHelper.checkNotNullArgument(executor, "executor");
            ExceptionHelper.checkNotNullArgument(cancelExecutor, "cancelExecutor");
            ExceptionHelper.checkNotNullArgument(channelOpener, "channelOpener");
            ExceptionHelper.checkNotNullArgument(channelProcessor, "channelProcessor");

            this.executor = executor;
            this.cancelExecutor = cancelExecutor;
            this.channelOpener = channelOpener;
            this.channelProcessor = channelProcessor;
        }

        public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener) {
            final AsyncDataListener<DataType> safeListener;
            safeListener = AsyncHelper.makeSafeListener(dataListener);

            final SimpleStateListener dataState = new SimpleStateListener(cancelExecutor);

            ChannelProcessorTask<?, ?> task = new ChannelProcessorTask<>(
                    channelOpener, channelProcessor, safeListener, dataState);
            final Future<?> taskFuture = executor.submit(task);

            return new AsyncDataController() {
                @Override
                public void controlData(Object controlArg) {
                }

                @Override
                public void cancel() {
                    try {
                        if (taskFuture != null) {
                            taskFuture.cancel(true);
                        }
                        dataState.cancel();
                    } finally {
                        safeListener.onDoneReceive(AsyncReport.CANCELED);
                    }
                }

                @Override
                public AsyncDataState getDataState() {
                    return dataState.getState();
                }
            };
        }
    }

    private static class ChannelProcessorTask<DataType, ChannelType extends Channel>
    implements
            Runnable {

        private final ChannelOpener<? extends ChannelType> channelOpener;
        private final ChannelProcessor<DataType, ChannelType> channelProcessor;
        private final AsyncDataListener<DataType> safeListener;
        private final SimpleStateListener stateListener;

        public ChannelProcessorTask(
                ChannelOpener<? extends ChannelType> channelOpener,
                ChannelProcessor<DataType, ChannelType> channelProcessor,
                AsyncDataListener<DataType> safeListener,
                SimpleStateListener stateListener) {
            this.channelOpener = channelOpener;
            this.channelProcessor = channelProcessor;
            this.safeListener = safeListener;
            this.stateListener = stateListener;
        }

        @Override
        public void run() {
            Throwable error = null;
            boolean canceled = false;
            try (ChannelType channel = channelOpener.openChanel()) {
                stateListener.setChannel(channel);
                if (!stateListener.isCanceled()) {
                    channelProcessor.processChannel(channel, safeListener, stateListener);
                }
                else {
                    canceled = true;
                }
            } catch (ClosedChannelException ex) {
                canceled = true;
            } catch (Throwable ex) {
                error = ex;
            } finally {
                stateListener.setChannel(null);
                safeListener.onDoneReceive(AsyncReport.getReport(error, canceled));
            }
        }
    }

    private static class SimpleStateListener
    implements
            ChannelProcessor.StateListener {

        private final ExecutorService cancelExecutor;
        // "channelToCancel" may be set to non-null at most once
        private volatile Channel channelToCancel;
        private final AtomicBoolean canceledChannel;
        private volatile AsyncDataState currentState;
        private volatile boolean canceled;

        public SimpleStateListener(ExecutorService cancelExecutor) {
            this.cancelExecutor = cancelExecutor;
            this.currentState = new SimpleDataState("Task was submitted to process the channel.", 0.0);
            this.channelToCancel = null;
            this.canceledChannel = new AtomicBoolean(false);
            this.canceled = false;
        }

        public void setChannel(Channel channel) {
            channelToCancel = channel instanceof InterruptibleChannel
                    ? channel
                    : null;

            if (canceled) {
                tryCancelCurrentChannel();
            }
        }

        private void closeCurrentChannel() {
            Channel channel = channelToCancel;
            if (channel != null) {
                try {
                    channel.close();
                } catch (Throwable ex) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                                Level.FINE,
                                "Closing the channel to cancel has failed: " + channel,
                                ex);
                    }
                }
            }
        }

        private void tryCancelCurrentChannel() {
            if (channelToCancel != null) {
                if (!canceledChannel.getAndSet(true)) {
                    cancelExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            closeCurrentChannel();
                        }
                    });
                }
            }
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void cancel() {
            canceled = true;
            tryCancelCurrentChannel();
        }

        @Override
        public void setState(AsyncDataState state) {
            this.currentState = state;
        }

        public AsyncDataState getState() {
            return currentState;
        }
    }
}
