//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health.stats;

import java.time.Instant;
import java.util.List;

/**
 * Triple store volumetry snapshot.
 *
 * @author Arnaud Charleroy
 */
public record TripleStoreVolumetry(
        Long tripleCount,
        Integer graphCount,
        List<GraphVolumetry> graphs,
        Instant computedAt,
        String message) {

    public record GraphVolumetry(String graph, Long tripleCount) {
    }
}
