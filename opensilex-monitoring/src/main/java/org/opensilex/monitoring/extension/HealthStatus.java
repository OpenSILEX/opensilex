//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.extension;

/**
 * State of one monitored component.
 *
 * <p>{@link #UNKNOWN} is deliberately distinct from {@link #DOWN}: a probe that could not be run
 * is not the same claim as a component that answered badly, and conflating the two cries wolf
 * during a transient hiccup.</p>
 *
 * @author Arnaud Charleroy
 */
public enum HealthStatus {

    UP,
    DEGRADED,
    DOWN,
    UNKNOWN;

    /**
     * @return the worse of two statuses, used to fold component states into an overall one
     */
    public HealthStatus worst(HealthStatus other) {
        return other == null || this.ordinal() >= other.ordinal() ? this : other;
    }
}
