//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.activity;

import java.time.Instant;

/**
 * One point of the activity series.
 *
 * <p>Client and server errors are kept apart on purpose. A raw failure percentage mixes a user
 * mistyping a filter with the triple store falling over, and only the second one is an alert.</p>
 *
 * @param bucket       start of the time bucket
 * @param users        distinct accounts that called during the bucket
 * @param requests     calls recorded during the bucket
 * @param clientErrors calls answered 4xx
 * @param serverErrors calls answered 5xx, or ended in an unhandled exception
 *
 * @author Arnaud Charleroy
 */
public record ActivityBucket(Instant bucket, long users, long requests,
                             long clientErrors, long serverErrors) {

    public long errors() {
        return clientErrors + serverErrors;
    }

    public double errorPercentage() {
        return requests == 0 ? 0d : (errors() * 100d) / requests;
    }
}
