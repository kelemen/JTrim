Swing Components
================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-swing-component:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-swing-component</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-image-query"
  - "org.jtrim2:query"
    - "org.jtrim2:jtrim-executor"
      - "org.jtrim2:jtrim-concurrent"
        - "org.jtrim2:jtrim-collections"
          - "org.jtrim2:jtrim-utils"
    - "org.jtrim2:jtrim-cache"
  - "org.jtrim2:jtrim-image"
- "org.jtrim2:jtrim-image-transform"
- "org.jtrim2:jtrim-swing-query"
  - "org.jtrim2:jtrim-ui-query"
    - "org.jtrim2:jtrim-ui-concurrent"
      - "org.jtrim2:jtrim-access"
  - "org.jtrim2:jtrim-swing-concurrent"
- "org.jtrim2:jtrim-swing-property"


Description
-----------

Rendering a *Swing* component is always done on the EDT. This can get
problematic because sometimes, rendering a component is an expensive operation.
JTrim may help with this by providing the `AsyncRenderingComponent` class. This
is a *Swing* component which lets you define a renderer which will be called on
a background thread (where it is actually called can be easily configured by the
client code) to do the rendering. What you are usually expected to do is
whenever a property of the component changes affecting the rendering, you should
replace the renderer. You shouldn't worry about overflowing the component,
because it is impossible - using the classes in JTrim - to overflow the
background renderer. This component also supports asynchronously retrieved
inputs for the rendering out of the box relying on the data links of the
*jtrim-query* module.


### Core classes ###

- `AsyncRenderingComponent`: A base Swing component providing asynchronous
  rendering support.
- `TransformedImageDisplay`: A base Swing component able to display an
  asynchronously loaded image after a series of user defined transformations.
