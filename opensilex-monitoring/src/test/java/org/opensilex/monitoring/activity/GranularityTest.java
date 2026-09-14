//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

/**
 * @author Arnaud Charleroy
 */
public class GranularityTest {

    @Test
    public void hourlyBucketsOverSixMonthsBlowPastAnyReasonableCap() {
        // The number this guard exists for: nobody can read four thousand points, and asking
        // MongoDB for them is a gift to whoever hand-writes the URL.
        long buckets = Granularity.HOUR.bucketCount(Duration.ofDays(183));
        assertTrue("expected more than 4000 buckets, got " + buckets, buckets > 4000);
    }

    @Test
    public void hourlyBucketsOverAMonthStayUnderAThousand() {
        assertTrue(Granularity.HOUR.bucketCount(Duration.ofDays(31)) <= 1000);
    }

    @Test
    public void dailyBucketsOverTwoYearsStayUnderAThousand() {
        assertTrue(Granularity.DAY.bucketCount(Duration.ofDays(730)) <= 1000);
    }

    @Test
    public void coarserGranularitiesComeLaterSoPromotionIsJustOrdinalOrder() {
        assertTrue(Granularity.HOUR.ordinal() < Granularity.DAY.ordinal());
        assertTrue(Granularity.DAY.ordinal() < Granularity.WEEK.ordinal());
        assertTrue(Granularity.WEEK.ordinal() < Granularity.MONTH.ordinal());
    }

    @Test
    public void mongoUnitsMatchTheDateTruncVocabulary() {
        assertEquals("hour", Granularity.HOUR.getMongoUnit());
        assertEquals("day", Granularity.DAY.getMongoUnit());
        assertEquals("week", Granularity.WEEK.getMongoUnit());
        assertEquals("month", Granularity.MONTH.getMongoUnit());
    }

    @Test
    public void errorPercentageCountsClientAndServerErrorsTogetherAndSurvivesAnEmptyBucket() {
        ActivityBucket bucket = new ActivityBucket(Instant.now(), 3, 200, 15, 5);
        assertEquals(20, bucket.errors());
        assertEquals(10d, bucket.errorPercentage(), 0.0001);

        ActivityBucket empty = new ActivityBucket(Instant.now(), 0, 0, 0, 0);
        assertEquals(0d, empty.errorPercentage(), 0.0001);
    }
}
