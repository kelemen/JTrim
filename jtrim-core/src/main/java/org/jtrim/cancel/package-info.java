/**
 * Defines classes and interface related to canceling tasks or other operations.
 * <P>
 * <h3>The Java way</h3>
 * In most Java classes, waiting for a specific task or operation can be
 * canceled using thread interrupts. Using thread interrupts however is not easy
 * to done right and as such is error prone. In fact many (even respected)
 * libraries hide thrown {@code InterruptedException} instances and with it the
 * interrupted status of a thread. Note that the
 * {@code java.util.concurrent.FutureTask} class is prone to interrupt another
 * task running on the same thread later. Theoretically it is also possible
 * (but requires an extremely unlucky thread scheduling) that canceling a task
 * of {@code java.util.concurrent.ThreadPoolExecutor} will cause another task of
 * the {@code ThreadPoolExecutor} to be interrupted. Since even the classes in
 * the core library of Java is prone to such errors, it signifies the problems
 * using thread interruption.
 * <P>
 * The main problem with thread interruption is that it is effectively a global
 * variable which is almost completely public to anyone. This makes it hard to
 * rely on thread interrupts as anyone who has access to the thread (and every
 * task running on them. Another problem is that one cannot be notified of
 * thread interrupts asynchronously. The interrupted status of a thread can only
 * be polled causing an inherent limitation.
 *
 * <h3>Cancellation in JTrim</h3>
 * JTrim classes employ a way of canceling tasks or operations very similar to
 * <I>.NET 4</I>. That is, cancellation can be detected through a
 * {@link org.jtrim.cancel.CancellationToken}. This {@code CancellationToken}
 * can be controlled (i.e.: be canceled) by a
 * {@link org.jtrim.cancel.CancellationController}. See those classes for
 * further details on how exactly.
 * <P>
 * Since there are many useful classes in Java already relying on thread
 * interruptions, the {@link org.jtrim.cancel.CancelableWaits} helper class was
 * created to provide static helper methods to convert waiting for thread
 * interrupts to waiting for {@code CancellationToken}.
 *
 * @see org.jtrim.cancel.CancelableWaits
 * @see org.jtrim.cancel.CancellationSource
 */
package org.jtrim.cancel;
