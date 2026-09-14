//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.extension;

/**
 * Outcome of a single health probe.
 *
 * <p>A record rather than a bean: it is an internal value type that never crosses the API
 * boundary, where the swagger generator would need getters.</p>
 *
 * @param status     component state
 * @param durationMs how long the probe took, wall clock
 * @param message    human readable detail, {@code null} when there is nothing to say
 *
 * @author Arnaud Charleroy
 */
public record ProbeResult(HealthStatus status, long durationMs, String message) {

    public static ProbeResult up(long durationMs) {
        return new ProbeResult(HealthStatus.UP, durationMs, null);
    }

    public static ProbeResult degraded(long durationMs, String message) {
        return new ProbeResult(HealthStatus.DEGRADED, durationMs, message);
    }

    public static ProbeResult down(long durationMs, String message) {
        return new ProbeResult(HealthStatus.DOWN, durationMs, message);
    }

    public static ProbeResult unknown(String message) {
        return new ProbeResult(HealthStatus.UNKNOWN, 0L, message);
    }
}
