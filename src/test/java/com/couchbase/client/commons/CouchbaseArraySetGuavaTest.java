package com.couchbase.client.commons;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import junit.framework.TestSuite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseArraySet} using guava-testlib's testsuite
 * generator for sets.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CouchbaseArraySetGuavaTest.GuavaTests.class })
public class CouchbaseArraySetGuavaTest {

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
    public static class GuavaTests  {

        private static String uuid;

        public static TestSuite suite() {
            TestSuite suite = new SetTestSuiteBuilder<Object>()
                    .using(new TestSetGenerator<Object>() {
                        @Override
                        public Set<Object> create(Object... elements) {
                            CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, bucket, null);
                            for (Object o : elements) {
                                set.add(o);
                            }
                            return set;
                        }

                        @Override
                        public SampleElements<Object> samples() {
                            return GuavaTestUtils.samplesWithoutJsonValues;
                        }

                        @Override
                        public Object[] createArray(int length) {
                            return new Object[length];
                        }

                        @Override
                        public Iterable<Object> order(List<Object> insertionOrder) {
                            return insertionOrder;
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
                    .named("CouchbaseArraySet")
                    .withFeatures(
                            SetFeature.GENERAL_PURPOSE,
                            CollectionFeature.RESTRICTS_ELEMENTS,
                            CollectionFeature.ALLOWS_NULL_VALUES,
                            CollectionSize.ANY)
                    .createTestSuite();
            return suite;
        }
    }
}
