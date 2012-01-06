package org.jtrim.access.task;

import java.util.concurrent.*;
import org.jtrim.access.AccessToken;

/**
 * Defines an executor which can execute {@link RewTask REW tasks}.
 * <P>
 * To execute a REW tasks the executor needs two
 * {@link AccessToken access tokens}: One for reading the input and one for
 * writing the output.
 * <P>
 * The executor will first read the {@link RewTask#readInput() input} in the
 * context of the specified read {@link org.jtrim.access.AccessToken AccessToken}
 * using its default {@link java.util.concurrent.Executor Executor} then start
 * {@link RewTask#evaluate(java.lang.Object, org.jtrim.access.task.RewTaskReporter) evaluating}
 * the output on an {@code Executor} which is defined for the REW task executor.
 * Finally it will write the output in the context of the write
 * {@code AccessToken} using its default {@code Executor}.
 * <P>
 * REW task executors can report the state of the evaluate part of the REW
 * task being executed if the task reports it. However in case the task reports
 * its progress or any data after the evaluate part the executor is not required
 * to execute those reports but is allowed to do so.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be completely thread-safe
 * and the methods can be called from any thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> but they must not wait for asynchronous
 * or external tasks to complete.
 *
 * @see RewTask
 * @see RewTaskExecutors
 * @author Kelemen Attila
 */
public interface RewTaskExecutor {
    /**
     * Executes the specified REW task asynchronously. The submitted task can be
     * canceled by canceling the returned {@code Future}.
     * <P>
     * Note that after canceling the REW task it may still execute some part of
     * the REW task despite that the returned {@code Future} may signal that the
     * REW task was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     * <P>
     * The passed access tokens will not be shutted down and must be shutted
     * down manually. For easier and safer usage consider using the
     * {@link #executeAndRelease(org.jtrim.access.task.RewTask, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) executeAndRelease(RewTask<?, ?>, AccessToken<?>, AccessToken<?>)}
     * method.
     *
     * @param task the REW task to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW task. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW task and the reported progress of the evaluate
     *   part. This argument cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   task. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> execute(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken);

    /**
     * Executes the specified REW task asynchronously but reads the input of
     * the REW task on the current call stack immediately. The submitted task
     * can be canceled by canceling the returned {@code Future}.
     * <P>
     * Note that after canceling the REW task it may still execute some part of
     * the REW task despite that the returned {@code Future} may signal that the
     * REW task was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     * <P>
     * The passed access tokens will not be shutted down and must be shutted
     * down manually. For easier and safer usage consider using the
     * {@link #executeNowAndRelease(org.jtrim.access.task.RewTask, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) executeNowAndRelease(RewTask<?, ?>, AccessToken<?>, AccessToken<?>)}
     * method.
     *
     * @param task the REW task to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW task. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW task and the reported progress of the evaluate
     *   part. This argument cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   task. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> executeNow(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken);

    /**
     * Executes the specified REW task asynchronously and shutdowns the passed
     * access tokens whenever the REW task terminates. The access tokens
     * will be shutted down in any case, even if the REW task is canceled or
     * throws an exception. The submitted task can be canceled by canceling
     * the returned {@code Future}.
     * <P>
     * Note that after canceling the REW task it may still execute some part of
     * the REW task despite that the returned {@code Future} may signal that the
     * REW task was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     *
     * @param task the REW task to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW task. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW task and the reported progress of the evaluate
     *   part. This argument cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   task. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> executeAndRelease(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken);

    /**
     * Executes the specified REW task asynchronously but reads the input of
     * the REW task on the current call stack immediately and shutdowns the
     * passed access tokens whenever the REW task terminates. The access tokens
     * will be shutted down in any case, even if the REW task is canceled or
     * throws an exception. The submitted task can be canceled by canceling
     * the returned {@code Future}.
     * <P>
     * Note that after canceling the REW task it may still execute some part of
     * the REW task despite that the returned {@code Future} may signal that the
     * REW task was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     *
     * @param task the REW task to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW task. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW task and the reported progress of the evaluate
     *   part. This argument cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   task. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> executeNowAndRelease(RewTask<?, ?> task,
            AccessToken<?> readToken, AccessToken<?> writeToken);
}
