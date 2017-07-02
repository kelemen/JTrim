Task Graph Execution
====================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-task-graph:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-task-graph</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-executor"
  - "org.jtrim2:jtrim-concurrent"
    - "org.jtrim2:jtrim-collections"
      - "org.jtrim2:jtrim-utils"


Description
-----------

This module helps to organize a computation with complex dependencies. The
solution provides the following benefits:

- Completely type safe.
- There is no need to explicitly connect a computation with its required inputs
  (i.e., with its dependencies). The dependencies are connected automatically
  in a similar way to how usual dependency injection frameworks work.
- A computation is described by a generic interface, therefore it is easy to
  use AOP (with the help of `TaskExecutorAop`).
- It is possible to restrict concurrent resource usage of the computation.

The most abstract idea is to describe the computation with a graph. In this
graph, nodes represent an executable computation, and edges represent the
dependencies between computations (i.e., nodes).

Simply creating a graph node by node would not be too much help. So, instead of
directly creating the nodes themselves: Factories of the nodes can be defined
to create the nodes. This is beneficial because factories can also take an
additional argument when creating a node. Therefore, it is possible to have a
single factory define multiple different computation nodes, providing a natural
grouping of nodes. Also, having a factory allows to define factories even if
they won't be used.


### Steps of execution

1. You must create an instance of `TaskGraphDefConfigurer` (possibly via
   `TaskGraphExecutors`).
2. You must define the possible task node factories. Note that these factories
   just define what node is possible to be created. In themselves, they won't
   imply any node to be created.
3. Once, all the task factories were declared, you must create a
   `TaskGraphBuilder` by calling the `build` method.
4. You must define some nodes that you want to be created. That is, the
   computations which you actually want to execute. You don't need to add nodes
   that are merely needed by the computations you actually need. Required nodes,
   will be created on demand.
5. Once you have add all the nodes added, you must create a `TaskGraphExecutor`
   by calling the `buildGraph` method. The `buildGraph` method is asynchronous,
   therefore you will have to get the created `TaskGraphExecutor` using Java's
   `CompletionStage`.
6. Once you have a `TaskGraphExecutor`, you may configure some of its properties
   (or you can leave the default configuration).
7. When you have the `TaskGraphExecutor` properly configured, you must call its
   `execute` method.
8. The graph execution will complete asynchronously. Note that altough, the
   execution can return the result of some computation, you might not need this
   feature. That is, you can have "computation" nodes with side effect (e.g.,
   persisting the result somewhere).


### Resource constraints

This task graph execution framework understands two kinds of resource
constraints:

- The number of concurrently can be limited by using an appropriate executor
  to execute computation nodes. The executor can be different for every node.
- It is possible that the output of computations consume resources while
  referenced. Most notably, they might retain a considerable amount memory.
  Therefore, you might not want to execute nodes as soon as possible because
  their result have to be retained until all nodes depending on them is started
  to be executed. Implementations in this framework can restrict node execution
  (even allowing for custom logic). See `TaskGraphExecutors` for details.


### Core interfaces

- `TaskGraphDefConfigurer`: The starting interface to build a task graph and
  execute it.
- `TaskFactory`: A factory creating computation nodes.


### Core classes ###

- `TaskGraphExecutors`: A utility class, containing static factory methods
  to create task graph executors (with `TaskGraphDefConfigurer`).
- `TaskExecutorAop`: A utility class to help wrapping task nodes and do
  something before or after they get executed.
