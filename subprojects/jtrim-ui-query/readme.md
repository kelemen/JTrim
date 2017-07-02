Asynchronous Query in UI
========================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-ui-query:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-ui-query</artifactId>
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
- "org.jtrim2:jtrim-query"
  - "org.jtrim2:jtrim-cache"


Description
-----------

This module currently define two utilities to help with asynchronous queries.
See the next subsections.

### Asynchronous rendering

It is possible that rendering a component takes considerable amount of time.
Note that even if rendering takes only a few hundred milliseconds, users might
find your UI sluggish. `GenericAsyncRendererFactory` helps you render a
component in the background when the source of the input is given by an
asynchronous query. `GenericAsyncRendererFactory` also takes care of the issue,
if you change what you want to render faster than it can be rendered. In this
case, the previous rendering is canceled and only the most recent is continued
to be executed.


### Access controlled background queries

Similar to the *jtrim-ui-concurrent* module, you might want to controll if a
background query can be executed or not at the moment and prevent part of the
UI from being manipulated. You can use an instance of `BackgroundDataProvider`
to help you with your access control. This class relies heavily on the
*jtrim-access* module.

*Swing* applications should see the module *jtrim-swing-query* for
convenience factories.


### Core interfaces ###

- `AsyncRendererFactory`: Instances of `AsyncRendererFactory` define the
  renderer of a particular UI component.

### Core classes ###

- `BackgroundDataProvider`: Provides support for access controlled asynchronous
  queries.
- `GenericAsyncRendererFactory`: Defines an asynchronous renderer of a
  component, using an externally provided executor.
