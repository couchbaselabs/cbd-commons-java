# Couchbase Datastructures Commons - Java

# Overview
Currently the following is supported:

 - CouchbaseArrayList

# Usage

## CouchbaseArrayList
The list is backed by a JSON document where the toplevel is a `[]`. All operations that need to fetch the doc
and perform updates use CAS loops in the background.

```java
import com.couchbase.client.commons.CouchbaseArrayList;

CouchbaseArrayList list = new CouchbaseArrayList("my-list-docid", bucket);

list.size();

list.isEmpty();

list.add("hello");
list.add(1234);
list.add(true);

list.remove(1);

list.contains(true);
```