Caching
=======

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-cache:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-cache</artifactId>
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

The caching API of JTrim is not a general purpose caching mechanism (nothing
like JSR 107). Instead, you could think of the caching API as a generalization
of weak and soft references of Java where you can provide your own
implementation when an object may be garbage collected. The two main interfaces
are `VolatileReference` and ObjectCache. You are expected to always be ready to retrieve a data but you may also store the retrieved data as a `VolatileReference`. The `VolatileReference` - depending on the implementation - may keep a reference to the object but may also "lose" this reference. How you can create a `VolatileReference`? You should have an ObjectCache which manages and creates `VolatileReference` instances to objects. In the simple case, the ObjectCache can choose to return weak or soft references of an object wrapped in a `VolatileReference` but may also have its own implementation and logic how the cache allows the referred object of `VolatileReference` instances to disappear. Obviously, using `VolatileReference` instances is only reasonable when the object to be cached retains lots of memory (large array, image, etc.).

### Core interfaces

- `ObjectCache`: Manages cached objects.
- `VolatileReference`: A generalization of weak and soft references.
  
### Core class

- `MemorySensitiveCache`: An implementation of ObjectCache.
