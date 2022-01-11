/**
 * Contains classes and interfaces for asynchronous data retrieval. Asynchronous
 * data retrieval is necessary when a particular part of the code cannot block
 * and wait but needs to retrieve a data for its operation. Such is the case
 * with the AWT event dispatch thread, which must not be blocked to avoid frozen
 * GUIs.
 *
 * <h2>Data links</h2>
 * {@link org.jtrim2.concurrent.query.AsyncDataLink Data links} are the
 * fundamental part of the package, they represent a connection to a specific
 * data. Through this connection, the data can be retrieved as many times as
 * required.
 * <P>
 * The base interface for data links is {@link org.jtrim2.concurrent.query.AsyncDataLink}
 * which was designed to be simple to implement and simple to use. To implement
 * it, one must implement its lone method to retrieve the data in a separate
 * thread and notify the specified {@link org.jtrim2.concurrent.query.AsyncDataListener listener}
 * when the data is available. A usually sufficient implementation is
 * to submit a task to a {@link org.jtrim2.executor.TaskExecutorService TaskExecutorService}
 * and retrieve the data synchronously in that task and when the data is finally
 * available, notify the listener.
 * <P>
 * Implementations should do only a single task which is as simple as possible
 * and should not do any extra work like caching. Such extra works can and
 * should be implemented in separate implementations and those implementations
 * should then be combined. For common tasks like a data link transforming
 * the result of another data link, caching the result of a data link: the class
 * {@link org.jtrim2.concurrent.query.AsyncLinks} contains convenient factory
 * methods.
 *
 * <h2>Data queries</h2>
 * Since data links represent a connection to a single particular data and one
 * does not always want to retrieve the same data over and over,
 * {@link org.jtrim2.concurrent.query.AsyncDataQuery data queries} were created.
 * <P>
 * The base interface for data queries is {@link org.jtrim2.concurrent.query.AsyncDataQuery}
 * and usually they are even more easier to implement than data links. Since
 * they are designed to return a data link for a specific input, a usual
 * implementation only needs to create and return a new instance of a data link.
 * That is, in most cases they are just simply a factory of data links.
 * <P>
 * Just like data links, it is possible to create a complex data query
 * implementation by combining multiple instances. The class
 * {@link org.jtrim2.concurrent.query.AsyncQueries} contains convenient factory
 * methods for useful implementations.
 *
 * <h2>Debugging</h2>
 * Debugging concurrent code is always harder than debugging a simple
 * synchronous code. Asynchronous data links are no exceptions. Since usually
 * implementing data links are just submitting a task to an executor and then
 * do the data retrieval process synchronously, it should be relatively easy to
 * test and debug such implementations. If it is desirable to avoid putting
 * break points to the start of such synchronous code, one could use an executor
 * which executes tasks synchronously (assuming executors are used as
 * recommended) in the test code and simply single step the code. Although
 * executing tasks synchronously is different and may not detect synchronization
 * issues, it may help to rule out such issue in specific cases. Also due to the
 * {@code AsyncDataLink} interface being simple, synchronization issues are less
 * likely to occur.
 *
 * @see org.jtrim2.concurrent.query.AsyncDataLink
 * @see org.jtrim2.concurrent.query.AsyncDataQuery
 *
 * @see org.jtrim2.concurrent.query.AsyncHelper
 * @see org.jtrim2.concurrent.query.AsyncLinks
 * @see org.jtrim2.concurrent.query.AsyncQueries
 */
package org.jtrim2.concurrent.query;
