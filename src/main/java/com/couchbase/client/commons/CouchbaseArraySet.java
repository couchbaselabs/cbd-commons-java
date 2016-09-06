package com.couchbase.client.commons;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.subdoc.DocumentFragment;

/**
 * A CouchbaseArraySet is a {@link Set} backed by a {@link Bucket Couchbase} document (more
 * specifically a {@link JsonArrayDocument JSON array}).
 *
 * Note that a CouchbaseArraySet is restricted to primitive types (the types that a {@link JsonArray JSON array}
 * can contain, except {@link JsonObject} and {@link JsonArray}). null entries are supported.
 *
 * @param <T> the type of values in the set.
 */
public class CouchbaseArraySet<T> extends AbstractSet<T> {

    public static final int MAX_OPTIMISTIC_LOCKING_ATTEMPTS = CouchbaseArrayList.MAX_OPTIMISTIC_LOCKING_ATTEMPTS;

    private final String id;
    private final Bucket bucket;

    /**
     * Create a new {@link CouchbaseArraySet}, backed by the document identified by <code>id</code>
     * in the given Couchbase <code>bucket</code>. Note that if the document already exists,
     * its content will be used as initial content for this collection. Otherwise it is created empty.
     *
     * @param id the id of the Couchbase document to back the set.
     * @param bucket the {@link Bucket} through which to interact with the document.
     */
    public CouchbaseArraySet(String id, Bucket bucket) {
        this.id = id;
        this.bucket = bucket;

        try {
            this.bucket.insert(JsonArrayDocument.create(id, JsonArray.empty()));
        } catch (DocumentAlreadyExistsException e) {
            //use a pre-existing document
        }
    }

    /**
     * Create a new {@link CouchbaseArraySet}, backed by the document identified by <code>id</code>
     * in the given Couchbase <code>bucket</code>. Note that if the document already exists, its content is
     * reset to the values copied from the given <code>data</code> Map.
     *
     * A null or empty data map will re-initialize any pre-existing document to an empty content.
     *
     * @param id the id of the Couchbase document to back the set.
     * @param bucket the {@link Bucket} through which to interact with the document.
     * @param initialData Set of the elements to initially store in the CouchbaseArraySet.
     */
    public CouchbaseArraySet(String id, Bucket bucket, Set<? extends T> initialData) {
        this.id = id;
        this.bucket = bucket;

        JsonArray data = JsonArray.create();
        if (initialData != null && !initialData.isEmpty()) {
            for (Object o : initialData) {
                enforcePrimitive(o);
                data.add(o);
            }
        }
        bucket.upsert(JsonArrayDocument.create(id, data));
    }

    @Override
    public int size() {
        //TODO use subdoc GET_COUNT when available
        JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
        return current.content().size();
    }

    @Override
    public boolean isEmpty() {
        DocumentFragment<Lookup> current = bucket.lookupIn(id).exists("[0]").execute();
        return current.status(0) == ResponseStatus.SUBDOC_PATH_NOT_FOUND;
    }

    @Override
    public boolean contains(Object t) {
        //TODO subpar implementation for a Set, use ARRAY_CONTAINS when available
        enforcePrimitive(t);
        JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
        for (Object in : current.content()) {
            if (safeEquals(in, t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
        return new Itr(current);
    }

    @Override
    public boolean add(T t) {
        enforcePrimitive(t);

        for (int i = 0; i < MAX_OPTIMISTIC_LOCKING_ATTEMPTS; i++) {
            try {
                JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
                long cas = current.cas();
                //Care not to use toList, as it will convert internal JsonObject/JsonArray to Map/List
                boolean absent = true;
                for (Object in : current.content()) {
                    if (safeEquals(in, t)) {
                        absent = false;
                        break;
                    }
                }

                if (absent) {
                    DocumentFragment<Mutation> result = bucket.mutateIn(id)
                            .arrayAppend("", t, true) //append at the root array
                            .withCas(cas)
                            .execute();
                    return true;
                } else {
                    return false;
                }
            } catch (CASMismatchException e) {
                //retry
            }
        }
        throw new ConcurrentModificationException("Couldn't perform add in less than " + MAX_OPTIMISTIC_LOCKING_ATTEMPTS + " iterations");
    }

    @Override
    public boolean remove(Object t) {
        enforcePrimitive(t);

        for (int i = 0; i < MAX_OPTIMISTIC_LOCKING_ATTEMPTS; i++) {
            try {
                JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
                long cas = current.cas();
                int index = 0;
                boolean found = false;
                Iterator<Object> it = current.content().iterator();
                while (it.hasNext()) {
                    Object next = it.next();
                    if (safeEquals(next, t)) {
                        found = true;
                        break;
                    }
                    index++;
                }
                String path = "[" + index + "]";

                if (!found) {
                    return false;
                } else {
                    DocumentFragment<Mutation> result = bucket
                            .mutateIn(id).remove(path).withCas(cas).execute();
                    return true;
                }
            } catch (CASMismatchException e) {
                //retry
            }
        }
        throw new ConcurrentModificationException("Couldn't perform remove in less than " + MAX_OPTIMISTIC_LOCKING_ATTEMPTS + " iterations");
    }

    @Override
    public void clear() {
        bucket.upsert(JsonArrayDocument.create(id, JsonArray.empty()));
    }

    /**
     * Verify that the type of object t is compatible with CouchbaseArraySet storage.
     *
     * @param t the object to check.
     * @throws ClassCastException if the object is incompatible.
     */
    protected void enforcePrimitive(Object t) throws ClassCastException {
        if (!JsonValue.checkType(t)
                || t instanceof JsonValue) {
            throw new ClassCastException("Only primitive types are supported in CouchbaseArraySet, got a " + t.getClass().getName());
        }
    }

    protected boolean safeEquals(Object expected, Object tested) {
        if (expected == null) {
            return tested == null;
        }
        return expected.equals(tested);
    }

    private class Itr implements Iterator<T> {

        private long cas;
        private final Iterator<T> delegate;
        private int lastVisited = -1;
        private boolean doneRemove = false;

        public Itr(JsonArrayDocument current) {
            this.cas = current.cas();
            this.delegate = (Iterator<T>) current.content().iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public T next() {
            if (hasNext()) {
                lastVisited++;
                doneRemove = false;
            }
            return delegate.next();
        }

        @Override
        public void remove() {
            if (lastVisited < 0) {
                throw new IllegalStateException("Cannot remove before having started iterating");
            }
            //skip remove attempts past the first one after a next()
            if (doneRemove) {
                throw new IllegalStateException("Cannot remove twice in a row while iterating");
            }
            String path = "[" + lastVisited + "]";
            //use the cas to attempt to remove
            try {
                DocumentFragment<Mutation> itrRemoveResult = bucket.mutateIn(id)
                        .withCas(cas)
                        .remove(path)
                        .execute();
                //update the cas
                this.cas = itrRemoveResult.cas();
                //ok the remove succeeded in DB, let's reflect that in the iterator's backing collection and state
                delegate.remove();
                doneRemove = true;
                lastVisited--;
            } catch (CASMismatchException e) {
                throw new ConcurrentModificationException("Couldn't remove while iterating: " + e);
            } catch (MultiMutationException e) {
                if (e.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new IllegalStateException("Invalid remove index " + path);
                }
                throw e;
            }
        }
    }
}
