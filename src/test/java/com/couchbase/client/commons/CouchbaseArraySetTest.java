package com.couchbase.client.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CouchbaseArraySetTest {

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

    private String uuid;

    @Before
    public void generateId() {
        uuid = uuid();
    }

    @After
    public void deleteDoc() {
        try {
            bucket.remove(uuid);
        } catch (DocumentDoesNotExistException e) {
            //ignore
        }
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void shouldRefuseAddingJsonObjectToSet() {
        CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, bucket, null);

        try {
            set.add(JsonObject.create());
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("CouchbaseArraySet"));
            //success
        }
    }

    @Test
    public void shouldRefuseCreatingSetWithJsonObject() {
        Set<Object> initial = Collections.<Object>singleton(JsonObject.create());

        try {
            CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, bucket, initial);
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("CouchbaseArraySet"));
            //success
        }
    }

    @Test
    public void shouldAddCloseValuesDifferentTypes() {
        CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, bucket);

        set.add("1");
        set.add(1);
        set.add(1.0);

        assertEquals(3, set.size());
    }
}
