Asynchronous Computation in UIs
===============================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-ui-concurrent:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-ui-concurrent</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-access"
  - "org.jtrim2:jtrim-property"
    - "org.jtrim2:jtrim-executor"
      - "org.jtrim2:jtrim-concurrent"
        - "org.jtrim2:jtrim-collections"
          - "org.jtrim2:jtrim-utils"


Description
-----------

This module helps with executing long running operations in the background. The
implementation relies on the *jtrim-access* module for resource access control.
You can create a `BackgroundTaskExecutor` which lets you track if a background
task execution is in progress and even block parts of the UI from being accessed
by the user.

*Swing* applications should see the module *jtrim-swing-concurrent* for
convenience factories.


### Core interface

- `BackgroundTask`: Defines a background operation which might notify the UI
  of its progress, or when it completes.

### Core class

- `BackgroundTaskExecutor`: A background executor, executing `BackgroundTask`
  instances with a given resource constraint (i.e., tasks might be blocked from
  being executed).
