New Collections
===============

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-collections:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-collections</artifactId>
            <version>${jtrimVersion}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Dependencies
------------

- "org.jtrim2:jtrim-utils"


Description
-----------

This module provides additional classes and interfaces to work with
collections. The two main interfaces here are `RefCollection` and `RefList`.
They allow you to keep a reference to elements added to a collection and use
this reference to adjust the position of this element in a list or remove it
from a collection. These interfaces were created mainly to allow exploitation of
all the benefits of a linked list (e.g.: remove an element which you have added
in constant time). Note however, that a new implementation of `PriorityQueue`
could also benefit from the `RefCollection` interface.


### Core interface

- `RefList`: A List of which you can easily and efficiently manipulate its
  elements after adding them.
  
### Core class

- `RefLinkedList`: An implementation of `RefList`.
