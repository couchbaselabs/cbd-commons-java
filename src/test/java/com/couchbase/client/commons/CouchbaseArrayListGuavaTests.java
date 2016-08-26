package com.couchbase.client.commons;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import junit.framework.TestSuite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseArrayList} using guava-testlib's testsuite
 * generator for lists.
 *
 * @author Simon Baslé
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CouchbaseArrayListGuavaTests.GuavaTests.class })
public class CouchbaseArrayListGuavaTests {

    private static Cluster cluster;
    private static Bucket bucket;

    @BeforeClass
    public static void initCluster() {
        cluster = CouchbaseCluster.create();
        bucket = cluster.openBucket();
    }

    @AfterClass
    public static void disconnect() {
        cluster.disconnect();
    }

    //the holder for the guava-generated test suite
    public static class GuavaTests {

        private static String uuid;

        public static TestSuite suite() {
            TestSuite suite = new ListTestSuiteBuilder<String>()
                    .using(new TestStringListGenerator() {
                        @Override
                        protected List<String> create(String[] elements) {
                            CouchbaseArrayList<String> l = new CouchbaseArrayList<String>(uuid, bucket, elements);
                            return l;
                        }
                    })
                    .withSetUp(new Runnable() {
                        @Override
                        public void run() {
                            uuid = UUID.randomUUID().toString();
                        }
                    })
                    .withTearDown(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                bucket.remove(uuid);
                            } catch (DocumentDoesNotExistException e) {
                                //ignore
                            }
                        }
                    })
                    .named("CouchbaseArrayList")
                    .withFeatures(
                            ListFeature.SUPPORTS_SET,
                            CollectionFeature.SUPPORTS_ADD,
                            CollectionFeature.SUPPORTS_REMOVE,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            ListFeature.SUPPORTS_ADD_WITH_INDEX,
                            ListFeature.SUPPORTS_REMOVE_WITH_INDEX,
                            CollectionFeature.RESTRICTS_ELEMENTS,
                            CollectionFeature.ALLOWS_NULL_VALUES,
                            CollectionSize.ANY)
                    .createTestSuite();
            return suite;
        }
    }
}
