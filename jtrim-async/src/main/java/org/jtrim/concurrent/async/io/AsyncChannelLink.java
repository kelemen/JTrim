package org.jtrim.concurrent.async.io;

import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.*;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@code AsyncDataLink} which allows to read an arbitrary
 * {@link Channel} and process its content to be provided as asynchronously.
 * <P>
 * The implementations relies on two executors, namely:
 * {@code processorExecutor} and {@code cancelExecutor}. When the data is
 * requested from {@code AsyncChannelLink} it will submit a task to
 * {@code processorExecutor} which will actually
 * {@link ChannelOpener#openChanel() open} a new channel which is then
 * {@link ChannelProcessor#processChannel(Channel, AsyncDataListener, ChannelProcessor.StateListener) processed}
 * in the same task. In this task will the {@link AsyncDataListener} be notified
 * of the data to be provided.
 * <P>
 * When a data retrieval request is canceled through the returned
 * {@link AsyncDataController}, the basic action taken is to cancel the task
 * submitted to the {@code processorExecutor} and if the task has not yet been
 * started processing the channel, it will be canceled without actually starting
 * to process it. When however, the opened channel implements the
 * {@link InterruptibleChannel}, it will even be asynchronously closed, so reads
 * while processing the channel will cause {@link ClosedChannelException} to be
 * thrown which is interpreted as a cancellation. Since closing the channel
 * might be an external call, it is actually done in a task submitted to
 * {@code cancelExecutor}.
 * <P>
 * Note that although actual canceling of the processing of a channel may take
 * some time, the {@code AsyncChannelLink} listening for the data will see as
 * if requesting a cancel request is almost instantaneous regardless how long
 * actually the canceling takes.
 *
 * <h3>Thread safety</h3>
 * As required by {@code AsyncDataLink}, methods of this class are safe to be
 * accessed by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of data to be provided by this
 *   {@code AsyncDataLink}
 *
 * @see ChannelOpener
 * @see ChannelProcessor
 *
 * @author Kelemen Attila
 */
public final class AsyncChannelLink<DataType> implements AsyncDataLink<DataType> {
    private static final Logger LOGGER = Logger.getLogger(AsyncChannelLink.class.getName());

    private final CheckedAsyncChannelLink<? extends DataType, ?> impl;

    /**
     * Creates a new {@code AsyncChannelLink} with the specified executors,
     * {@code ChannelOpener} and {@code ChannelProcessor}.
     *
     * @param <ChannelType> the type of the channel to be opened by the
     *   {@code channelOpener} and then processed by {@code channelProcessor}
     * @param processorExecutor the executor to which tasks will be submitted
     *   to process a channel opened by {@code channelOpener}. This argument
     *   may not be {@code null}.
     * @param cancelExecutor the executor to which tasks will be submitted to
     *   asynchronously close channels if the request for processing it has been
     *   canceled. This executor can be the same as {@code processorExecutor},
     *   note however that in this case, the processing of the channel may
     *   block the task which is submitted to cancel it (by closing the
     *   channel). This argument cannot be {@code null}.
     * @param channelOpener the {@code ChannelOpener} which must open a new
     *   channel to be processed by {@code channelProcessor}. The
     *   {@code ChannelOpener} must always open a channel to the same source
     *   (e.g.: with files, to the same file) to preserve the contract of the
     *   {@code AsyncDataLink}. This argument cannot be {@code null}.
     * @param channelProcessor the {@code ChannelProcessor} which must process
     *   open channels and forward the data it reads from it to the
     *   {@code AsyncDataLink}. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public <ChannelType extends Channel> AsyncChannelLink(
            TaskExecutorService processorExecutor,
            TaskExecutor cancelExecutor,
            ChannelOpener<? extends ChannelType> channelOpener,
            ChannelProcessor<? extends DataType, ChannelType> channelProcessor) {

        this.impl = new CheckedAsyncChannelLink<>(
                processorExecutor,
                cancelExecutor,
                channelOpener,
                channelProcessor);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: The
     * {@link AsyncDataController#getDataState() state of progress} of the data
     * retrieval process can possibly be a different type of object than the one
     * provided by the {@link ChannelProcessor channel processor}.
     */
    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super DataType> dataListener) {
        return impl.getData(cancelToken, dataListener);
    }

    private static class CheckedAsyncChannelLink<DataType, ChannelType extends Channel> {
        private final TaskExecutorService processorExecutor;
        private final TaskExecutor cancelExecutor;
        private final ChannelOpener<? extends ChannelType> channelOpener;
        private final ChannelProcessor<DataType, ChannelType> channelProcessor;

        public CheckedAsyncChannelLink(
                TaskExecutorService processorExecutor,
                TaskExecutor cancelExecutor,
                ChannelOpener<? extends ChannelType> channelOpener,
                ChannelProcessor<DataType, ChannelType> channelProcessor) {

            ExceptionHelper.checkNotNullArgument(processorExecutor, "processorExecutor");
            ExceptionHelper.checkNotNullArgument(cancelExecutor, "cancelExecutor");
            ExceptionHelper.checkNotNullArgument(channelOpener, "channelOpener");
            ExceptionHelper.checkNotNullArgument(channelProcessor, "channelProcessor");

            this.processorExecutor = processorExecutor;
            this.cancelExecutor = cancelExecutor;
            this.channelOpener = channelOpener;
            this.channelProcessor = channelProcessor;
        }

        public AsyncDataController getData(
                CancellationToken cancelToken,
                AsyncDataListener<? super DataType> dataListener) {

            if (cancelToken.isCanceled()) {
                // Return quickly on early cancel
                dataListener.onDoneReceive(AsyncReport.CANCELED);
                return DoNothingDataController.INSTANCE;
            }

            final AsyncDataListener<DataType> safeListener;
            safeListener = AsyncHelper.makeSafeListener(dataListener);

            final SimpleStateListener dataState = new SimpleStateListener(cancelExecutor);

            ChannelProcessorTask<?, ?> task = new ChannelProcessorTask<>(
                    channelOpener, channelProcessor, safeListener, dataState);

            final ListenerRef listenerRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    dataState.cancel();
                }
            });

            processorExecutor.submit(cancelToken, task, new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    listenerRef.unregister();
                }
            });

            return new AsyncDataController() {
                @Override
                public void controlData(Object controlArg) {
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
            CancelableTask {

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
        public void execute(CancellationToken cancelToken) {
            Throwable error = null;
            boolean canceled = false;
            try (ChannelType channel = channelOpener.openChanel()) {
                stateListener.setChannel(channel);
                if (!cancelToken.isCanceled()) {
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

        private final TaskExecutor cancelExecutor;
        // "channelToCancel" may be set to non-null at most once
        private volatile Channel channelToCancel;
        private final AtomicBoolean canceledChannel;
        private volatile AsyncDataState currentState;
        private volatile boolean canceled;

        public SimpleStateListener(TaskExecutor cancelExecutor) {
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
                    cancelExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                        @Override
                        public void execute(CancellationToken cancelToken) {
                            closeCurrentChannel();
                        }
                    }, null);
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
