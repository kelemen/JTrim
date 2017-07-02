Asynchronous Queries in Swing
=============================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-swing-query:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-swing-query</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-swing-concurrent"
  - "org.jtrim2:jtrim-ui-concurrent"
    - "org.jtrim2:jtrim-access"
      - "org.jtrim2:jtrim-property"
        - "org.jtrim2:jtrim-executor"
          - "org.jtrim2:jtrim-concurrent"
            - "org.jtrim2:jtrim-collections"
              - "org.jtrim2:jtrim-utils"
- "org.jtrim2:jtrim-ui-query"
  - "org.jtrim2:jtrim-query"

Description
-----------

This module simply provides *Swing* specific factories for the
*jtrim-ui-query* module. That is, it helps with asynchronous data retrieval
in *Swing*.


### Core class ###

- `SwingQueries`: Contains factories for asynchronous data retrieval.
