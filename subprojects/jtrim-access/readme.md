Resource Access Management
==========================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-access:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-access</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-property"
  - "org.jtrim2:jtrim-executor"
    - "org.jtrim2:jtrim-concurrent"
      - "org.jtrim2:jtrim-collections"
        - "org.jtrim2:jtrim-utils"


Description
-----------

The aim of this module is to manage resource access between cooperating agents.
These classes were explicitly designed to manage the background tasks of a UI.

Since you don't want to execute (possibly) expensive calls on the UI thread,
you will have to execute some tasks asynchronously. While it is not too bad to
execute a task asynchronously and then execute additional task on the UI later,
it is by far not the only concern (despite that most utilities help you to
solve this already simple problem). There are a couple of harder problems to
consider:

- Whenever your asynchronous computation completes, you will probably want to
  display something on the UI. You must ask you yourself: In what state the UI
  will be when the computation completes? The best thing to assume, that it
  might be in any state. The component where you would display your result might
  have been removed from the component tree (or is simply hidden). In any case,
  crashing is not the answer when you find the UI in an unexpected state. Your
  goal should be to prevent the user from accidentally losing the value of a
  computation, if it was explicitly requested.
- It might be possible that you cannot allow a user to start a particular
  backround task. Reasons for this include: the result of the task would
  (partially) overwrite the result of another currently running computation or
  the user needs to specify a valid input.
- One word: cancellation. Whenever a task takes more than just a few seconds to
  complete, you had better allowing the user to cancel that task. It is not you
  who needs to decide how important the result of the computation, you must
  allow the user to back out, as he or she might think that this time might be
  better spent on something else.

So how does JTrim address this issue? The core interface which you should look
for is `AccessManager`. Despite the name, it has nothing to do with security.
This interface was designed for tasks willing to cooperate. An `AccessManager`
is used to manage abstract resources (called rights in the API doc). Access to
an abstract resource is represented by an `AccessToken`. The main feature of an
`AccessToken` is that it allows executing tasks on a `TaskExecutor` ensuring
that the abstract resource remains available during the execution of the task.
That is, while such task is executing, noone can acquire the abstract resource.
What can this so called "abstract resource" be? In short, anything you wish but
usually it should be a right to execute a background task. So in this case, you
might not want to allow a task to be executed multiple times concurrently.


### Core interfaces ###

- `AccessManager`: Manages the access to abstract resources.
- `AccessToken`: Represents an acquired abstract resource and allows executing
  tasks ensuring that the resource remains acquired while the task executes.
  This is usually created by an `AccessManager`.

### Core classes ###

- `HierarchicalAccessManager`: A relatively generic implementation of
  `AccessManager`.
- `ScheduledAccessToken`: Acquires access to a token right after another given
  token was released.
