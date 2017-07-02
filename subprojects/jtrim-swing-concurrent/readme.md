Concurrenct Utilities for Swing
===============================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-swing-concurrent:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-swing-concurrent</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-ui-concurrent"
  - "org.jtrim2:jtrim-access"
    - "org.jtrim2:jtrim-property"
      - "org.jtrim2:jtrim-executor"
        - "org.jtrim2:jtrim-concurrent"
          - "org.jtrim2:jtrim-collections"
            - "org.jtrim2:jtrim-utils"


Description
-----------

Contains generic concurrency utility useful for *Swing* application. This module
builds on the generic UI concurrency utility provided by the
*jtrim-ui-concurrent* module.

### Core class

- `SwingExecutors`: Defines factory methods to create various kinds of
  executors, executing tasks on the event dispatch thread.
