//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.activity;

import java.time.Duration;

/**
 * Bucket size of an activity report.
 *
 * @author Arnaud Charleroy
 */
public enum Granularity {

    HOUR("hour", Duration.ofHours(1)),
    DAY("day", Duration.ofDays(1)),
    WEEK("week", Duration.ofDays(7)),
    MONTH("month", Duration.ofDays(30));

    private final String mongoUnit;
    private final Duration approximateSize;

    Granularity(String mongoUnit, Duration approximateSize) {
        this.mongoUnit = mongoUnit;
        this.approximateSize = approximateSize;
    }

    /** @return the unit understood by the MongoDB {@code $dateTrunc} expression */
    public String getMongoUnit() {
        return mongoUnit;
    }

    /**
     * @return how many buckets a period of this length would produce, used to refuse a report
     *         nobody could read before it reaches MongoDB
     */
    public long bucketCount(Duration period) {
        long size = approximateSize.toMillis();
        return size <= 0 ? 0 : (period.toMillis() / size) + 1;
    }
}
