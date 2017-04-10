package org.jtrim.taskgraph;

import java.util.function.Consumer;
import org.jtrim.event.ListenerRef;

public interface TaskGraphFuture<R> {
    public ListenerRef onComplete(Consumer<? super R> handler);
}
