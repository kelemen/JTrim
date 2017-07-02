Asynchronous Queries
====================

Artifact ID
-----------

### Gradle

    dependencies {
        compile "org.jtrim2:jtrim-query:${jtrimVersion}"
    }

### Maven

    <dependencies>
        <dependency>
            <groupId>org.jtrim2</groupId>
            <artifactId>jtrim-query</artifactId>
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
- "org.jtrim2:jtrim-cache"


Description
-----------

### Data links

A so called data link is represented by an `AsyncDataLink` object in JTrim. Data
links define the data and the means to retrieve it. A simple example is an URL
and a browser: You can click on a link in the browser to retrieve the data. Also
even if you click multiple times on the link, you will get the same data
(although the webserver might provide different file, let's just assume it does
not). The same is with `AsyncDataLink`, once you have an instance of it, you can
retrieve the same data over and over. A notable additional feature of data link
is that it can provide partial data until the complete data is available. For
example, you might want to download and display an image: It might be
advantegous for you to display only parts of the image until the complete image
is available.

The simplicity of data links allows to easily combine them. For example, one
data link might retrieve a data and another one can be used to convert that
data. Also, a data link might wrap another one, relying on it to actually
retrieve the data and then it may caches what the wrapped link retrieves
(actually there is an implementation for this in JTrim). You can create many
useful data links by the factory methods in `AsyncLinks`.


### Queries

Data queries are simple factories of data link objects. They take an input and
return a data link based on the given input argument. Staying with the URL
example, you can pass an URL to a query and it will return a data link for you
which you can use to retrieve the file stored on that URL. The interface for
these queries in JTrim is `AsyncDataQuery`. The main reason to have this factory
interface because they allow for powerful ways to combine them. You can easily
connect two queries if one of the query takes as an input, the output of the
other query. For useful built-in implementations, see the `AsyncQueries` class
for factory methods.


### Core interfaces ###

- `AsyncDataLink`: Represents a data and the means to retrieve it.
- `AsyncDataQuery`: Represents an asynchronous query of a data.

### Core classes ###

- `AsyncLinks`: Factory methods for useful data links.
- `AsyncQueries`: Factory methods for useful data queries.
