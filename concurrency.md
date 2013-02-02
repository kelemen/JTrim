Documenting thread safety
=========================

When documenting access in a multithreaded environment most documentation only
states that a particular object can or cannot be accessed from multiple threads
simultaneously. Others define different levels of thread safety such as those
found in *Effective Java 2nd Edition*. However I still find these
classifications insufficient because these only consider possible state
corruptions and ignore other threading hazards. The most significant problem is
that they ignore the fact that in many cases the synchronization that a code
uses is not transparent to the client. While it would be desirable to a client
to be oblivious to any synchronization control that a library code uses, it is
not always possible. In fact it is far more common that both the library and
the client code can access the same synchronization controls unintentionally
than one would think from the complete absence of this kind of documentation.
Ignoring these can cause deadlocks and other subtle problems. First I will
consider possibilities how synchronization can be visible using simple locks
then I will describe other less apparent ways to leak such
"implementation details".

The easiest way to surprise a client code is to use synchronized methods
because this is effectively the same as synchronizing on the `this` reference
(in Java every object is a reentrant lock and can also act as a `Condition` for
this lock). Since the `this` reference is obviously visible to the caller (it
can be considered as a public lock object), a malicious or careless client can
easily introduce a deadlock. This kind of synchronization can also cause
subtler bugs such as the one that can be found in *Java Puzzlers (Puzzle 77)*.
For this reason I would forbid locking on publicly available locks. I also
strongly discourage the use of intrinsic locks in favor of `ReentrantLock`.
Note that it is still possible to share a `ReentrantLock` or an object
exclusively used for locking but luckily most programmer are sane enough not to
do so.

A less apparent but more frequent way to introduce the possibility of a
deadlock is to hold a lock (`LOCK1`) while executing a code that can be defined
by the client. Such code can easily introduce the dreaded lock reordering: The
alien code may try to aquire a lock (`LOCK2`) meanwhile another thread of the
client code may also lock on `LOCK2` then calls a method that needs to hold
`LOCK1`. The two threads might end up waiting for each other to release the
locks, which is a classic lock reordering problem. In my experience it is
always unsafe to try to reason that "such thing cannot happen" therefore I
suggest that an alien code must never be called while holding a lock (no matter
how hard to implement it).

One of the most rarely documented detail is that on which thread (or threads)
the code can possibly call methods provided by the client. A notable example
for the possible cause of such missing documentation is that `SwingWorker` does
not document that on what thread it calls the `doInBackground` method. The
first implementation in the JDK delegated this method call to a cached thread
pool which could spawn boundless number of threads but in a later version of
the JDK this thread pool was changed to a single threaded thread pool. Since
many users implemented the `doInBackground` method that it waited for the
result of another `SwingWorker`, it ended up waiting forever (because the
second `SwingWorker` would never start as there was no free thread to start the
dependency). Needless to say that this generated quite a lot of bug report
(e.g: [Bug 6880336](http://bugs.sun.com/view_bug.do?bug_id=6880336)). So to
avoid this I highly recommend to document on what thread a client code may
be called, even if it is called on the current call stack of method of an api
call. From the perspective of clients, I recommend to be as defensive as
possible and if there is even a slight doubt on which thread a method may be
called: expect the worst.


Definitions
===========

If a client code can distinguish a method invocation from an atomic operation
which waits for an unknown amount of time using the method only in allowed
contexts (as defined by its specification): The method is said to be leaking
synchronization information. The information leaking is defined as
**Synchronization transparency**. If no such distinction can be made the method
is said to be *synchronization transparent*.

A class is said to be *synchronization transparent* if and only if all of its
methods are *synchronization transparent*. Note that subclasses may define
methods that are not *synchronization transparent*, the only requirement is
that if they implement or override a method, this method must be
*synchronization transparent* as well.

- **Lock transparency**: A special case of *synchronization transparency* where a
  client code can detect that the method uses a certain lock (reentrant or
  non-reentrant).
- **Thread transparency**: A special case of *synchronization transparency*
  where a client code can detect that the method uses a certain threads
  directly or indirectly (e.g.: dispatches a task to limited size thread pool).
- **Dependency transparency**: A special case of *thread transparency* where a
  client code can detect that the method waits for some tasks to complete
  scheduled to a thread. This transparency can be overlooked very easily and
  cause a deadlock if the method waits for a task that is scheduled to the
  calling thread. Notice that this can also occur if the calling thread is a
  thread of a limited size thread pool and the task is scheduled to this thread
  pool because calling this method simultaneously can block the thread pool.

Assumptions in Java
------------------

- The `equals(Object)` and `hashcode()` methods of every object are
  *synchronization transparent*.
- `Collection` interface is *synchronization transparent*.
- `Throwable` is *synchronization transparent*.
- The constructor of every `Throwable` is *synchronization transparent*.
