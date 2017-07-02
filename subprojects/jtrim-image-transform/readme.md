BufferedImage Transformation
============================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-image-transform:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-image-transform</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-concurrent"
  - "org.jtrim2:jtrim-collections"
    - "org.jtrim2:jtrim-utils"
- "org.jtrim2:jtrim-image"
  - "org.jtrim2:jtrim-cache"


Description
-----------

This module contains utilities to transform `BufferedImage` instances with some
commonly used transformation. The transformation are applied given a limited
size destination in mind (usually a UI component). The implementations and
interfaces also consider optimizations to avoid creating new buffers needlessly.


### Core classes

- `AffineTransformationStep`: Applies an affine transformation to a given image.
- `ZoomToFitTransformationStep`: Applies a zoom to fit transformation to a given
  image. Magnifying or shrinking the image depending on the configuration and
  the destination size.
