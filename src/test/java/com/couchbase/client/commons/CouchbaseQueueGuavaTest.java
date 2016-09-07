package com.couchbase.client.commons;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.QueueTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestQueueGenerator;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests the functionality of {@link CouchbaseQueue} using guava-testlib's testsuite
 * generator for queues.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CouchbaseQueueGuavaTest.GuavaTests.class })
public class CouchbaseQueueGuavaTest {

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
            TestSuite suite = QueueTestSuiteBuilder.using(
                    new TestQueueGenerator<Object>() {
                        @Override
                        public Queue<Object> create(Object... elements) {
                            return new CouchbaseQueue<Object>(uuid, bucket, elements);
                        }

                        @Override
                        public SampleElements<Object> samples() {
                            return GuavaTestUtils.samples;
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
                            testCount--;
                            if (testCount < 1) {
                                cluster.disconnect();
                            }
                        }
                    })
                    .named("CouchbaseQueue")
                    .withFeatures(
                            CollectionFeature.KNOWN_ORDER,
                            CollectionFeature.SUPPORTS_ADD,
                            CollectionFeature.SUPPORTS_REMOVE,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            CollectionFeature.RESTRICTS_ELEMENTS,
                            CollectionSize.ANY)
                    .createTestSuite();

            testCount = suite.countTestCases() - suite.testCount();
            return suite;
        }
    }

}
