//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.extension;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Arnaud Charleroy
 */
public class HealthStatusTest {

    @Test
    public void foldingPicksTheWorstStatus() {
        assertEquals(HealthStatus.DOWN, HealthStatus.UP.worst(HealthStatus.DOWN));
        assertEquals(HealthStatus.DOWN, HealthStatus.DOWN.worst(HealthStatus.UP));
        assertEquals(HealthStatus.DEGRADED, HealthStatus.UP.worst(HealthStatus.DEGRADED));
    }

    @Test
    public void unknownIsWorseThanDownSoAnUnrunnableProbeIsNeverReportedAsHealthy() {
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.DOWN.worst(HealthStatus.UNKNOWN));
    }

    @Test
    public void foldingAgainstNothingKeepsTheCurrentStatus() {
        assertEquals(HealthStatus.UP, HealthStatus.UP.worst(null));
    }
}
