Logging (JUL) utilities
=======================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-logs:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-logs</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-collections"
  - "org.jtrim2:jtrim-utils"


Description
-----------

This module adds utilities for *java.util.logging*. These utilities are usually
useful to test logging.


### Core class

- `LogCollector`: Collects logs during its life time. This class is intended
  to test if the tested code produced logs as it should have.
