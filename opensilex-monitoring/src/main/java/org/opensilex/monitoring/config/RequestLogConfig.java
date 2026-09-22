//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.config;

import java.util.List;
import org.opensilex.config.ConfigDescription;

/**
 * Web service access log tuning.
 *
 * @author Arnaud Charleroy
 */
public interface RequestLogConfig {

    @ConfigDescription(
            value = "Record every web service call in MongoDB",
            defaultBoolean = true
    )
    boolean enabled();

    @ConfigDescription(
            value = "Record how long each call took. Switchable because measuring is not free and"
                    + " some instances only care about who called what.",
            defaultBoolean = true
    )
    boolean recordDuration();

    @ConfigDescription(
            value = "Record query parameters. Only ever applies to GET calls: a request body is"
                    + " never read, because the authentication filter already re-buffers it and"
                    + " doing it twice is both a bug source and a memory cost.",
            defaultBoolean = true
    )
    boolean recordQueryParameters();

    @ConfigDescription(
            value = "Record the client IP address. Off by default: an IP is personal data, and a"
                    + " health dashboard does not need one.",
            defaultBoolean = false
    )
    boolean recordClientIp();

    @ConfigDescription(
            value = "Also record calls made without a valid account. Keep it on, otherwise every"
                    + " 401 disappears and the failure percentage is biased downwards.",
            defaultBoolean = true
    )
    boolean logAnonymous();

    @ConfigDescription(
            value = "Query parameter names whose value is replaced by '***' before storage",
            defaultList = {"password", "new_password", "token", "authorization", "api_key", "apiKey", "secret"}
    )
    List<String> maskedParameterNames();

    @ConfigDescription(
            value = "Maximum number of query parameters kept for one call",
            defaultInt = 32
    )
    int maxCapturedParameters();

    @ConfigDescription(
            value = "Maximum length of a stored query parameter value",
            defaultInt = 256
    )
    int maxParameterValueLength();

    @ConfigDescription(
            value = "Paths whose calls are never logged. 'monitoring' is mandatory: without it,"
                    + " reading the activity chart inflates the activity chart.",
            defaultList = {"monitoring", "vuejs/extension", "vuejs/theme", "core/system/info", "swagger"}
    )
    List<String> excludedPathPrefixes();

    @ConfigDescription(
            value = "Retention in days, applied as a MongoDB TTL index. 183 days is about six"
                    + " months. Zero or less disables expiry entirely.",
            defaultInt = 183
    )
    int retentionDays();

    @ConfigDescription(
            value = "Capacity of the in-memory queue. Once full, entries are dropped rather than"
                    + " blocking the request thread, and the drop is reported by the health check.",
            defaultInt = 10000
    )
    int queueCapacity();

    @ConfigDescription(
            value = "Number of entries written per insertMany",
            defaultInt = 200
    )
    int batchSize();

    @ConfigDescription(
            value = "Delay between two flushes, in milliseconds",
            defaultInt = 1000
    )
    int flushIntervalMs();

    @ConfigDescription(
            value = "Consecutive write failures before the writer stops trying",
            defaultInt = 3
    )
    int maxConsecutiveFailures();

    @ConfigDescription(
            value = "How long the writer stays open-circuit after tripping, in seconds",
            defaultInt = 60
    )
    int failureBackoffSeconds();
}
