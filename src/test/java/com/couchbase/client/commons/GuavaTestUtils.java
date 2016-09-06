package com.couchbase.client.commons;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.common.collect.testing.SampleElements;

/**
 * An abstract base for guava tests that has a uuid field (to be filled by implementations)
 * and exposes a small representative test sample.
 */
public abstract class GuavaTestUtils {

    private GuavaTestUtils() { }

    protected static SampleElements<Object> samples = new SampleElements<Object>(
            123,
            "aString",
            true,
            JsonObject.create().put("sub", "value"),
            JsonArray.from("A" ,"B", "C")
    );

    protected static SampleElements<Object> samplesWithoutJsonValues = new SampleElements<Object>(
            123,
            "aString",
            true,
            4.56,
            "foo"
    );
}
