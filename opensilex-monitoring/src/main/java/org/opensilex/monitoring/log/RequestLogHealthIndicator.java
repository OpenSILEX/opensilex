//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log;

import org.opensilex.monitoring.extension.HealthIndicator;
import org.opensilex.monitoring.extension.ProbeResult;

/**
 * Reports on the access log writer itself.
 *
 * <p>The writer drops entries rather than blocking the API, which is the right trade — but a silent
 * drop is a lie by omission. Surfacing it here is what makes the trade honest.</p>
 *
 * @author Arnaud Charleroy
 */
public class RequestLogHealthIndicator implements HealthIndicator {

    public static final String NAME = "requestLog";

    private final RequestLogService writer;

    public RequestLogHealthIndicator(RequestLogService writer) {
        this.writer = writer;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ProbeResult probe() {
        if (!writer.isRunning()) {
            return ProbeResult.down(0L, "access log writer is not running");
        }
        if (writer.isPaused()) {
            return ProbeResult.degraded(0L, "writer paused after repeated MongoDB write failures");
        }
        long dropped = writer.getDroppedCount();
        if (dropped > 0) {
            return ProbeResult.degraded(0L, dropped + " entries dropped since startup");
        }
        return ProbeResult.up(0L);
    }
}
