/**
 * Contains classes and interfaces for managing concurrent access to limited
 * resources. This package is not intended to be used for security and it was
 * designed only for cooperating tasks to share resources safely. The resources
 * can be many things like TCP connections or Swing components but this package
 * was mainly designed for GUIs to detect that a task conflict with already
 * running tasks. Note however that this package only contains general
 * interfaces and instances not directly related to GUIs.
 *
 * <h3>Access managers</h3>
 * Access managers are responsible for managing rights, so that no conflicting
 * rights are available to users concurrently. The base interface for access
 * managers is: {@link org.jtrim2.access.AccessManager}. See its description for
 * further details.
 *
 * <h3>Access tokens</h3>
 * Access tokens represent rights to a given resource. It is possible to create
 * {@code TaskExecutor} instances which execute tasks only if their access token
 * has not been released (i.e.: still available to the client code). These
 * tokens are usually created by access managers and will only be useful until
 * they are released. After an access token was released owners no longer hold
 * the rights represented by this access token unless they acquire another such
 * token. The base interface for access tokens is:
 * {@link org.jtrim2.access.AccessToken}. See its description for further
 * details.
 *
 * <h3>Tracking the availability of rights</h3>
 * You might want to track if a right can be acquired from an
 * {@code AccessManager} or not. For example, you might use this information to
 * disable or enable a GUI component. To support this, you should use boolean
 * properties. Boolean properties can be created for tracking the availability
 * of rights using the factory methods in {@link org.jtrim2.access.AccessProperties}.
 *
 * @see org.jtrim2.access.AccessManager
 * @see org.jtrim2.access.AccessProperties
 * @see org.jtrim2.access.AccessToken
 * @see org.jtrim2.access.HierarchicalAccessManager
 */
package org.jtrim2.access;
