/**
 * This package will be removed since the introduction of {@code TaskExecutor}
 * made classes and interfaces in this package no longer useful.
 *
 * Contains classes and interfaces for executing REW (read, evaluate, write)
 * queries.
 *
 * <h3>REW queries</h3>
 * REW queries are tasks that can be defined by three parts:
 * <ol>
 *   <li>Reading the input</li>
 *   <li>Querying a data using the previous input</li>
 *   <li>Writing the result of the query</li>
 * </ol>
 * The first part is executed in the context of a read
 * {@link org.jtrim.access.AccessToken AccessToken} while the last part
 * is executed in the context of a write {@code AccessToken}.
 * Additionally all REW queries can write (or display) the current
 * state of the query in the context of the write {@code AccessToken}.
 * <P>
 * The base interface which must be implemented by REW queries is:
 * {@link org.jtrim.access.query.RewQuery RewQuery}.
 *
 * <h3>REW query executors</h3>
 * Normally all REW queries are executed by a REW query executor. These
 * executors manage the life cycle of a REW query: Query the input, start
 * executing the query and write the result of the query (and its state).
 * <P>
 * The base interface which must be implemented by REW query executors is:
 * {@link org.jtrim.access.query.RewQueryExecutor RewQueryExecutor}.
 *
 * @see org.jtrim.access.query.RewQuery
 * @see org.jtrim.access.query.RewQueryExecutor
 * @see org.jtrim.access.query.AutoReportRewQueryExecutor
 */
package org.jtrim.access.query;
