package org.jtrim.access.query;

import java.util.concurrent.Future;
import org.jtrim.access.AccessToken;

/**
 * Defines an executor which can execute {@link RewQuery REW queries}.
 * <P>
 * To execute a REW query the executor needs two
 * {@link AccessToken access tokens}: One for reading the input for the query
 * and one for writing the output and the state of the query.
 * <P>
 * The executor first reads the {@link RewQuery#readInput() input} for the
 * {@link RewQuery#getOutputQuery() query} in the context of the specified
 * read access token then start retrieving data passing the input value to the
 * query. The results are
 * {@link RewQuery#writeOutput(Object) written out} in the context of the
 * specified write access token. Implementations may also
 * {@link RewQuery#writeState(org.jtrim.concurrent.async.AsyncDataState) report}
 * the state of the query. They must report it in the context of the write token
 * but how often and when they do so  at the discretion of the implementation.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be completely thread-safe
 * and the methods can be called from any thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> but they must not wait for asynchronous
 * or external tasks to complete.
 *
 * @see AutoReportRewQueryExecutor
 * @see RewQuery
 * @author Kelemen Attila
 */
public interface RewQueryExecutor {

    /**
     * Executes the specified REW query asynchronously. The submitted REW
     * query can be canceled by canceling the returned {@code Future} object.
     * <P>
     * Note that after canceling the REW query it may still execute the REW
     * query despite that the returned {@code Future} may signal that the
     * REW query was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     * <P>
     * Important note: <B>Shutting down any of the passed tokens before the
     * submitted query completes may cause the query to never finish.</B>
     * That is: The {@link Future#get() get method} of the returned future may
     * never completes successfully.
     *
     * @param query the REW query to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW query. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW query and the state of the query. This argument
     *   cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   query. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> execute(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken);

    /**
     * Executes the specified REW query asynchronously but reads the input of
     * the REW query on the current call stack immediately. The submitted REW
     * query can be canceled by canceling the returned {@code Future} object.
     * <P>
     * Note that after canceling the REW query it may still execute the REW
     * query despite that the returned {@code Future} may signal that the
     * REW query was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     * <P>
     * Note that since this method reads the input of the REW query on the
     * current call stack, it must wait for any tasks to which
     * {@link RewQuery#readInput()} waits.
     * <P>
     * Important note: <B>Shutting down any of the passed tokens before the
     * submitted query completes may cause the query to never finish.</B>
     * That is: The {@link Future#get() get method} of the returned future may
     * never completes successfully.
     *
     * @param query the REW query to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW query. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW query and the state of the query. This argument
     *   cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   query. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> executeNow(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken);

    /**
     * Executes the specified REW query asynchronously and shutdowns the passed
     * access tokens whenever the REW query terminates. The access tokens
     * will be shutted down in any case, even if the REW query is canceled or
     * throws an exception. The submitted REW query can be canceled by canceling
     * the returned {@code Future} object.
     * <P>
     * Note that after canceling the REW query it may still execute the REW
     * query despite that the returned {@code Future} may signal that the
     * REW query was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     * <P>
     * Important note: <B>Shutting down any of the passed tokens before the
     * submitted query completes may cause the query to never finish.</B>
     * That is: The {@link Future#get() get method} of the returned future may
     * never completes successfully.
     *
     * @param query the REW query to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW query. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW query and the state of the query. This argument
     *   cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   query. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> executeAndRelease(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken);

    /**
     * Executes the specified REW query asynchronously but reads the input of
     * the REW query on the current call stack immediately and shutdowns the
     * passed access tokens whenever the REW query terminates. The access tokens
     * will be shutted down in any case, even if the REW query is canceled or
     * throws an exception. The submitted REW query can be canceled by canceling
     * the returned {@code Future} object.
     * <P>
     * Note that after canceling the REW query it may still execute the REW
     * query despite that the returned {@code Future} may signal that the
     * REW query was canceled. This behaviour is the consequence of the
     * definition of the methods of the {@code Future} interface.
     * <P>
     * Note that since this method reads the input of the REW query on the
     * current call stack, it must wait for any tasks to which
     * {@link RewQuery#readInput()} waits.
     * <P>
     * Important note: <B>Shutting down any of the passed tokens before the
     * submitted query completes may cause the query to never finish.</B>
     * That is: The {@link Future#get() get method} of the returned future may
     * never completes successfully.
     *
     * @param query the REW query to be executed. This argument cannot be
     *   {@code null}.
     * @param readToken the access token to be used for reading the input
     *   of the specified REW query. This argument cannot be {@code null}.
     * @param writeToken the access token to be used for writing the result
     *   of the specified REW query and the state of the query. This argument
     *   cannot be {@code null}.
     * @return the future representing the completion of the specified REW
     *   query. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Future<?> executeNowAndRelease(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken);
}
