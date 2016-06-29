package com.couchbase.client.commons;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CouchbaseArrayListTest {

    private static Cluster cluster;
    private static Bucket bucket;

    @BeforeClass
    public static void setup() {
        cluster = CouchbaseCluster.create();
        bucket = cluster.openBucket();
    }

    @AfterClass
    public static void teardown() {
        cluster.disconnect();
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void size() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        assertEquals(0, list.size());
    }

    @Test
    public void isEmpty() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldAdd() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        assertFalse(list.contains("foobar"));
        list.add("foobar");
        assertTrue(list.contains("foobar"));
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void shouldRemoveByValue() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.add("foo");
        list.add("bar");
        assertTrue(list.contains("foo"));
        assertTrue(list.contains("bar"));
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());

        assertFalse(list.contains("blarb"));
        assertFalse(list.remove("blarb"));
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());

        assertTrue(list.remove("foo"));
        assertFalse(list.contains("foo"));
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());

        assertTrue(list.remove("bar"));
        assertFalse(list.contains("bar"));
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldRemoveByIndex() {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.add("foo");
        list.add("bar");
        list.add("baz");
        list.add(true);

        assertEquals(4, list.size());
        list.remove(1);
        assertEquals(3, list.size());
        assertEquals("baz", list.get(1));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldThrowOnOutOfBoundsRemove() {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.add("foo");
        list.add("bar");

        list.remove(14334324);
    }

    @Test
    public void shouldReturnIterator() {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.add("foo");
        list.add("bar");

        Iterator<Object> iter = list.iterator();
        int i = 0;
        while(iter.hasNext()) {
            Object obj = iter.next();
            assertTrue(obj instanceof String);
            switch(i) {
                case 0:
                    assertEquals("foo", obj);
                    break;
                case 1:
                    assertEquals("bar", obj);
                    break;
            }
            i++;
        }
        assertEquals(2, i);
    }

    @Test
    public void shouldClear() {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.add("foo");
        list.add("bar");
        assertEquals(2, list.size());

        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void shouldGet() {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.add("foo");
        list.add("bar");

        assertEquals("foo", list.get(0));
        assertEquals("bar", list.get(1));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldFailOnOutOfBoundsGet() {
        List<Object> list = new CouchbaseArrayList(uuid(), bucket);
        list.get(4234324);
    }

}