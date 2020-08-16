package org.jtrim2.mediator;

import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;

public final class AsyncJobDispatchers {
    public static <A, B> JobConsumerFactory<B> forwardToDispatcher(
            JobConsumerFactory<A> srcProcessorFactory,
            Supplier<? extends AsyncJobDispatcher<B>> step2DispatcherFactory) {

        Objects.requireNonNull(srcProcessorFactory, "srcProcessorFactory");
        Objects.requireNonNull(step2DispatcherFactory, "step2DispatcherFactory");

        return cancelToken -> {
            AsyncJobDispatcher<B> step2Dispatcher = step2DispatcherFactory.get();
            ListenerRef cancelListenerRef = cancelToken.addCancellationListener(step2Dispatcher::cancelProcessing);

            return new JobConsumer<B>() {
                @Override
                public void processJob(CancellationToken cancelToken, B job) throws Exception {
                    step2Dispatcher.dispatchJob(job);
                }

                @Override
                public void finishProcessing(ConsumerCompletionStatus finalStatus) throws Exception {
                    try {
                        step2Dispatcher.shutdownAndWait(Cancellation.UNCANCELABLE_TOKEN);
                    } finally {
                        cancelListenerRef.unregister();
                    }
                }
            };
        };
    }

    private AsyncJobDispatchers() {
        throw new AssertionError();
    }
}
