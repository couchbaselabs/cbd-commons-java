package com.couchbase.client.commons;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.subdoc.DocumentFragment;

import java.util.*;


public class CouchbaseArrayList extends AbstractList<Object> {

    private final String id;
    private final Bucket bucket;

    public CouchbaseArrayList(String id, Bucket bucket) {
        this.bucket = bucket;
        this.id = id;

        try {
            bucket.insert(JsonArrayDocument.create(id, JsonArray.empty()));
        } catch (DocumentAlreadyExistsException ex) {
            // Ignore concurrent creations, keep on moving.
        }
    }

    @Override
    public Object get(int index) {
        String idx = "[" + index + "]";

        DocumentFragment<Lookup> result = bucket.lookupIn(id).get(idx).execute();
        if (result.status(idx) == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        return result.content(idx);
    }

    @Override
    public int size() {
        return bucket.get(id, JsonArrayDocument.class).content().size();
    }

    @Override
    public Object set(int index, Object element) {
        if (!JsonValue.checkType(element)) {
            throw new IllegalArgumentException("Unsupported value type.");
        }
        String idx = "["+index+"]";

        while(true) {
            try {
                Object result = bucket.lookupIn(id).get(idx).execute().content(idx);
                bucket.mutateIn(id).replace(idx, element).execute();
                return result;
            } catch (CASMismatchException ex) {

            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new IndexOutOfBoundsException("Index: " + index);
                }
            }
        }
    }

    @Override
    public void add(int index, Object element) {
        if (!JsonValue.checkType(element)) {
            throw new IllegalArgumentException("Unsupported value type.");
        }
        bucket.mutateIn(id).arrayInsert("["+index+"]", element).execute();
    }

    @Override
    public Object remove(int index) {
        while(true) {
            try {
                bucket.mutateIn(id).remove("[" + index + "]").execute();
                return true;
            } catch (CASMismatchException ex) {

            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new IndexOutOfBoundsException("Index: " + index);
                }
            }
        }
    }
}
