/**
 * Contains classes and interfaces for managing concurrent access to limited
 * resources. This package is not intended to be used for security and it was
 * designed only for cooperating tasks to share resources safely. The resources
 * can be many things like TCP connections or Swing components but this package
 * was mainly designed for GUIs to detect that a task conflict with already
 * running tasks. Note however that this package only contains general
 * interfaces and instances not directly related to GUIs; for implementations
 * that help keep GUIs consistent see the {@code org.jtrim.swing.access}
 * package.
 *
 * <h3>Access managers</h3>
 * Access managers are responsible for managing rights, so that no conflicting
 * rights are available to users concurrently. The base interface for access
 * managers is: {@link org.jtrim.access.AccessManager}. See its description for
 * further details.
 *
 * <h3>Access tokens</h3>
 * Access tokens are executors (implementing
 * {@link java.util.concurrent.ExecutorService ExecutorService}) representing
 * certain rights. These tokens are usually created by access managers and
 * will only be useful until they are shutted down. After an access token was
 * shutted down owners no longer hold the rights represented by this access
 * token unless they aquire another such token. The base interface for access
 * tokens is: {@link org.jtrim.access.AccessToken}. See its description for
 * further details.
 *
 * @see org.jtrim.access.AccessManager
 * @see org.jtrim.access.AccessToken
 * @see org.jtrim.access.HierarchicalAccessManager
 */
package org.jtrim.access;
