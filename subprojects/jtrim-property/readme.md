Properties
==========

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-property:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-property</artifactId>
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

UI often contain a set of property describing how the component should be drawn.
Also, these properties may also change due to user input. In *Swing*, handling
of properties are usually done by getter and setter methods and changes in
properties are notified via the `PropertyChangeListener`. There are various
problems with this:

- When using getter and setter methods, it is hard to write common code to
  handle multiple properties. The usual solutions usually rely on reflection.
  However, reflection is error prone to use and not refactor friendly.
- `PropertyChangeListener` provides a string for which property has changed.
  Needing for a string to define the property is error prone and cannot be
  refactored automatically.
- There is no telling if `PropertyChangeListener` supports a particular property
  or not. Simply assuming that getter and setter methods imply support from the
  `PropertyChangeListener` is not the most robust idea.

*JavaFX* has considerably better property handling than *Swing*. However, even
*JavaFX* still retains the *addXXX*, *removeXXX* idiom of listener handling.
Since this module relies on the event handling mechanism of *jtrim-concurrent*,
it becomes considerably easier to handle heterogenous listener registrations.

Using properties, it is possible to simply create binding between states of the
UI and the value of the properties. For example, disabling parts of the window,
while a task is running. The module *jtrim-swing-property* adds lots of utility
to help with various bindings using when *Swing*.


### Core interfaces ###

- `PropertySource`: Defines a thread-safe value which might change outside the
  control of the client code.
- `MutableProperty`: A reference to a value which can be adjusted by the client
  code. It is also possible that the value changes outside the control of the
  client code.

### Core classes ###

- `PropertyFactory`: Contains lots of static factory methods for
 `PropertySource`, `MutableProperty` and related interfaces.
- `BoolProperties`: Contains static factory methods supporting properties with
  boolean value. Including logical operators (e.g.: *and*).
