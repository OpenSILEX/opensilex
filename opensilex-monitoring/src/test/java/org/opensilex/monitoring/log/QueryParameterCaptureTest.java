//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.bson.Document;
import org.junit.Test;
import org.opensilex.monitoring.config.RequestLogConfig;

/**
 * @author Arnaud Charleroy
 */
public class QueryParameterCaptureTest {

    private static MultivaluedMap<String, String> params() {
        return new MultivaluedHashMap<>();
    }

    private QueryParameterCapture capture(int maxParameters, int maxValueLength) {
        return new QueryParameterCapture(new TestConfig(maxParameters, maxValueLength));
    }

    @Test
    public void returnsNullRatherThanAnEmptyDocument() {
        assertNull(capture(32, 256).capture(null));
        assertNull(capture(32, 256).capture(params()));
    }

    @Test
    public void masksSensitiveValuesWhateverTheirCase() {
        MultivaluedMap<String, String> given = params();
        given.add("password", "hunter2");
        given.add("Token", "abc.def.ghi");
        given.add("name", "ZA17");

        Document result = capture(32, 256).capture(given);

        assertEquals("***", result.getString("password"));
        assertEquals("***", result.getString("Token"));
        assertEquals("ZA17", result.getString("name"));
    }

    @Test
    public void sanitisesKeysMongoWouldReject() {
        MultivaluedMap<String, String> given = params();
        given.add("a.b", "1");
        given.add("$where", "2");

        Document result = capture(32, 256).capture(given);

        assertTrue(result.containsKey("a_b"));
        assertTrue(result.containsKey("_where"));
    }

    @Test
    public void capsTheNumberOfParametersAndSaysSo() {
        MultivaluedMap<String, String> given = params();
        for (int i = 0; i < 10; i++) {
            given.add("p" + i, String.valueOf(i));
        }

        Document result = capture(3, 256).capture(given);

        assertEquals(Boolean.TRUE, result.get("_truncated"));
        assertEquals(4, result.size());
    }

    @Test
    public void capsTheLengthOfAValue() {
        MultivaluedMap<String, String> given = params();
        given.add("filter", "x".repeat(100));

        Document result = capture(32, 8).capture(given);

        assertEquals("xxxxxxxx…", result.getString("filter"));
    }

    @Test
    public void keepsRepeatedParametersAsAList() {
        MultivaluedMap<String, String> given = params();
        given.addAll("uri", "a", "b");

        Document result = capture(32, 256).capture(given);

        assertEquals(List.of("a", "b"), result.get("uri"));
    }

    /**
     * A hand-written stub rather than a mock: the interface has a dozen methods and only two of
     * them matter here, so a stub reads better than a dozen stubbing lines.
     */
    private record TestConfig(int maxParameters, int maxValueLength) implements RequestLogConfig {

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public boolean recordDuration() {
            return true;
        }

        @Override
        public boolean recordQueryParameters() {
            return true;
        }

        @Override
        public boolean recordClientIp() {
            return false;
        }

        @Override
        public boolean logAnonymous() {
            return true;
        }

        @Override
        public List<String> maskedParameterNames() {
            return List.of("password", "token");
        }

        @Override
        public int maxCapturedParameters() {
            return maxParameters;
        }

        @Override
        public int maxParameterValueLength() {
            return maxValueLength;
        }

        @Override
        public List<String> excludedPathPrefixes() {
            return List.of();
        }

        @Override
        public int retentionDays() {
            return 183;
        }

        @Override
        public int queueCapacity() {
            return 100;
        }

        @Override
        public int batchSize() {
            return 10;
        }

        @Override
        public int flushIntervalMs() {
            return 1000;
        }

        @Override
        public int maxConsecutiveFailures() {
            return 3;
        }

        @Override
        public int failureBackoffSeconds() {
            return 60;
        }
    }
}
