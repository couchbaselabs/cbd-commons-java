# Couchbase Datastructures Commons - Java

# Overview
Couchbase Datastructures Commons aims at providing datastructures backed by Couchbase in most languages
that have an official Couchbase SDK, while conforming to an existing *standard library* or equivalent in
each language.

For Java, this is the `Collection` API, as defined in **Java 6**. Note that the fact that these are backed
by Couchbase implies some restrictions on the keys/values that can be stored (see each collection's
javadoc for more details).

Currently the following Couchbase-backed `Collection`s are supported:

 - **CouchbaseArrayList** (a `List<T>`)
 - **CouchbaseMap** (a `Map<String, V>`)

# Usage

## CouchbaseArrayList
The list is backed by a JSON document where the toplevel is a `[]`. All operations that need to fetch the doc
and perform updates use CAS loops in the background.

```java
import com.couchbase.client.commons.CouchbaseArrayList;

//if the doc already exists, this constructor will re-use it
CouchbaseArrayList<Object> list = new CouchbaseArrayList<Object>("my-list-docid", bucket);

list.size();

list.isEmpty();

list.add("hello");
list.add(1234);
list.add(true);

list.remove(1);

list.contains(true);
```

## CouchbaseMap
The map is backed by a JSON document with a standard dictionary root `{}`. All operations that need to
fetch the doc and perform updates use CAS loops in the background.

```java
import com.couchbase.client.commons.CouchbaseMap;

//if the doc already exists, this constructor will re-use it
Map<String, Object> map = new CouchbaseMap<Object>("my-map-docid", bucket);

map.size();

map.isEmpty();

map.put("someString", "hello");
map.put("someBoolean", false);
map.put("subObject", JsonObject.create().put("subValue", "foo");

map.containsKey("someBoolean");
map.containsValue("hello");

map.get("subObject");
```