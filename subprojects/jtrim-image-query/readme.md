Asynchronous Image Query
========================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-image-query:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-image-query</artifactId>
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
- "org.jtrim2:jtrim-image"
  - "org.jtrim2:jtrim-cache"


Description
-----------

This module contains queries to load images asynchronously based on the
*jtrim-query* module. Java's ImageIO is used to load `BufferedImage` instances.


### Core classes

- `InputStreamImageLink`: Reads images from an input stream asynchronously.
- `UriImageIOQuery`: Queries images from an URI asynchronously.
