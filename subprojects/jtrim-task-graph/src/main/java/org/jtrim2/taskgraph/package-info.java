/**
 * Defines the core interface for the task graph execution framework. The framework
 * allows for dynamically defining a computations with arbitrary dependencies and
 * execute the computations without blocking using resources to the fullest.
 *
 * <h3>Building a task graph</h3>
 * <ol>
 *  <li>
 *   The possible factories of the computations (called task node factories:
 *   {@link org.jtrim2.taskgraph.TaskFactory TaskFactory}) must be declared.
 *   These factories only define what is possible to be computed not what
 *   actually will be computed.
 *  </li>
 *  <li>
 *   Once the task node factories were defined, some computation must
 *   be declared explicitly to be computed.
 *  </li>
 *  <li>
 *   After the initial required computations were declared, the framework
 *   will automatically create the task execution graph based on the dependencies
 *   of the computations.
 *  </li>
 *  <li>
 *   With the task graph ready, it can be executed, which will eventually
 *   notify its {@code CompletionStage} after all computations were completed.
 *  </li>
 * </ol>
 *
 * <h3>Resource constraints</h3>
 *
 * The framework allows restraining resource usage. There are two kinds of
 * resources, the framework recognizes:
 * <ul>
 *  <li>
 *   Resources being used only during a computation of a task node and released
 *   after the computation terminates. This resource usage can be limited by
 *   appropriate selection of the {@link org.jtrim2.concurrent.TaskExecutor task executors}.
 *  </li>
 *  <li>
 *   Resources associated with the output of computations. Outputs consume
 *   resources up until the point, no computation needs them. The management of
 *   such resources is the responsibility of the task graph execution implementation.
 *  </li>
 * </ul>
 *
 * @see org.jtrim2.taskgraph.TaskGraphDefConfigurer
 * @see org.jtrim2.taskgraph.TaskGraphExecutors
 */
package org.jtrim2.taskgraph;
