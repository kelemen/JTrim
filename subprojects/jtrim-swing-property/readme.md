Swing Properties
================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-swing-property:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-swing-property</artifactId>
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


Description
-----------

This module provides utility to bind *Swing* components to a property of
*JTrim* (defined by the *jtrim-property* module). Most notably, you can:

- Disable components while a boolean property is true. You can disable it simply
  by settings its *enabled* property to *false*. However, you can also disable
  by putting a glass pane over it to prevent access to a component. Putting a
  glass pane over the component, also allows for displaying a "Loading" text, or
  show a cancel button.
- See the values of the most common *Swing* components as a *JTrim* property.
- Convert between *Swing* and *JTrim* properties.


Note that the *jtrim-property* module allows you to construct complex boolean
properties.


### Core interface

- `SwingPropertySource`: Defines an interface which can easily wrap a property
  of a *Swing* component.

### Core classes ###

- `AutoDisplayState`: Contains static methods to conveniently set up automatic
  disabling and enabling (and many more) of components based on boolean
  properties.
- `SwingProperties`: Contains static factory methods converting *Swing*
  properties to *JTrim* style properties.
