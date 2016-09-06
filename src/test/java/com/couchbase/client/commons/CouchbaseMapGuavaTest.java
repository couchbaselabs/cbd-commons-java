package com.couchbase.client.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseMap} using guava-testlib's testsuite
 * generator for maps.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CouchbaseMapGuavaTest.GuavaTests.class })
public class CouchbaseMapGuavaTest {

    //the holder for the guava-generated test suite
    public static class GuavaTests {

        private static Cluster cluster = CouchbaseCluster.create();
        private static Bucket bucket = cluster.openBucket();
        private static int testCount;
        private static String uuid;

        @Test
        @Ignore
        //fixes "All Unit Tests" runs in IntelliJ complaining about no test method found
        public void noop() { }

        public static TestSuite suite() {
            TestSuite suite = new MapTestSuiteBuilder<String, String>()
                    .using(new TestStringMapGenerator() {
                        @Override
                        protected Map<String, String> create(Map.Entry<String, String>[] entries) {
                            HashMap<String, String> tempMap = new HashMap<String, String>(entries.length);
                            for (Map.Entry<String, String> entry : entries) {
                                tempMap.put(entry.getKey(), entry.getValue());
                            }
                            Map<String, String> map = new CouchbaseMap<String>(uuid, bucket, tempMap);
                            return map;
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
                            testCount--;
                            if (testCount < 1) {
                                cluster.disconnect();
                            }
                        }
                    })
                    .named("CouchbaseMap")
                    .withFeatures(
                            MapFeature.GENERAL_PURPOSE,
                            MapFeature.ALLOWS_NULL_VALUES,
                            MapFeature.RESTRICTS_KEYS,
                            MapFeature.RESTRICTS_VALUES,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            CollectionSize.ANY)
                    .createTestSuite();

            testCount = suite.countTestCases() - suite.testCount();
            return suite;
        }
    }
}
