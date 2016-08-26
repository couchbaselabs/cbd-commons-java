package com.couchbase.client.commons;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.subdoc.DocumentFragment;

import java.util.*;


public class CouchbaseArrayList<E> extends AbstractList<E> {

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
    public E get(int index) {
        String idx = "[" + index + "]";

        DocumentFragment<Lookup> result = bucket.lookupIn(id).get(idx).execute();
        if (result.status(idx) == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        return (E) result.content(idx);
    }

    @Override
    public int size() {
        //TODO in Spock, GET_COUNT should be available on subdoc
        JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
        return current.content().size();
    }

    @Override
    public boolean isEmpty() {
        DocumentFragment<Lookup> current = bucket.lookupIn(id).exists("[0]").execute();
        return current.status("[0]") == ResponseStatus.SUBDOC_PATH_NOT_FOUND;
    }

    @Override
    public E set(int index, E element) {
        if (!JsonValue.checkType(element)) {
            throw new IllegalArgumentException("Unsupported value type.");
        }
        String idx = "["+index+"]";

        while(true) {
            try {
                DocumentFragment<Lookup> current = bucket.lookupIn(id).get(idx).execute();
                long returnCas = current.cas();
                Object result = current.content(idx);
                bucket.mutateIn(id).replace(idx, element).withCas(returnCas).execute();
                return (E) result;
            } catch (CASMismatchException ex) {
                //will need to retry get-and-set
            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new IndexOutOfBoundsException("Index: " + index);
                }
            }
        }
    }

    @Override
    public void add(int index, E element) {
        if (!JsonValue.checkType(element)) {
            throw new IllegalArgumentException("Unsupported value type.");
        }
        bucket.mutateIn(id).arrayInsert("["+index+"]", element).execute();
    }

    @Override
    public E remove(int index) {
        String idx = "[" + index + "]";
        while(true) {
            try {
                DocumentFragment<Lookup> current = bucket.lookupIn(id).get(idx).execute();
                long returnCas = current.cas();
                Object result = current.content(idx);
                DocumentFragment<Mutation> updated = bucket.mutateIn(id).remove(idx).withCas(returnCas).execute();
                return (E) result;
            } catch (CASMismatchException ex) {
                //will have to retry get-and-remove
            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new IndexOutOfBoundsException("Index: " + index);
                }
            }
        }
    }

    @Override
    public boolean contains(Object o) {
        //TODO in Spock subdoc may have ARRAY_CONTAINS which can help implement indexOf, remove(Object) and contains
        return super.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new CouchbaseListIterator(0);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new CouchbaseListIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new CouchbaseListIterator(index);
    }

    @Override
    public void clear() {
        //optimized version over AbstractList's (which iterates on all and remove)
        bucket.upsert(JsonArrayDocument.create(id, JsonArray.empty()));
    }

    private class CouchbaseListIterator implements ListIterator<E> {

        private long cas;
        private final ListIterator<E> delegate;
        //flag set whenever next()/previous() is called, unset on remove()
        boolean canMutate;
        //the index to mutate, updated after a next()/previous()
        int lastVisited;
        //a flag indicating that remove() was called and lastVisited was already decremented
        //used by previous() to avoid decrementing the lastVisited index twice
        boolean hasRemoved;

        public CouchbaseListIterator(int index) {
            JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
            this.cas = current.cas();
            this.delegate = ((List<E>) current.content().toList()).listIterator(index);
            this.lastVisited = -1;
            this.canMutate = false;
            this.hasRemoved = false;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public E next() {
            lastVisited++;
            hasRemoved = false;
            canMutate = delegate.hasNext();
            return delegate.next();
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }

        @Override
        public E previous() {
            if (!hasRemoved)
                lastVisited--;
            hasRemoved = false;
            canMutate = delegate.hasPrevious();
            return delegate.previous();
        }

        @Override
        public int nextIndex() {
            return delegate.nextIndex();
        }

        @Override
        public int previousIndex() {
            return delegate.previousIndex();
        }

        @Override
        public void remove() {
            if (!canMutate) {
                throw new IllegalStateException();
            }
            int index = lastVisited;
            String idx = "[" + index + "]";
            try {
                DocumentFragment<Mutation> updated = bucket.mutateIn(id).remove(idx).withCas(this.cas).execute();
                //update the cas so that several removes in a row can work
                this.cas = updated.cas();
                //also correctly reset the state:
                delegate.remove();
                this.canMutate = false;
                this.lastVisited--;
                this.hasRemoved = true;
            } catch (CASMismatchException ex) {
                throw new ConcurrentModificationException("List was modified since iterator creation", ex);
            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new ConcurrentModificationException("Element doesn't exist anymore at index: " + index);
                }
            }
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
