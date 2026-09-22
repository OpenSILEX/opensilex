//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.extension;

/**
 * One thing worth probing.
 *
 * <p>Adding a component to the health check means adding one implementation of this interface,
 * never editing a switch. Implementations are free to throw: the runner turns any exception into
 * a {@link HealthStatus#DOWN} result and bounds the call in time, so an implementation does not
 * have to carry its own timeout.</p>
 *
 * @author Arnaud Charleroy
 */
public interface HealthIndicator {

    /**
     * @return stable identifier reported to the client, e.g. {@code "mongodb"}
     */
    String name();

    /**
     * Probe the component. Called from a bounded pool, never from the request thread.
     *
     * @return the outcome
     * @throws Exception any failure, translated to {@link HealthStatus#DOWN} by the runner
     */
    ProbeResult probe() throws Exception;
}
