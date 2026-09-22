//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health.stats;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB volumetry snapshot. Internal value type, mapped to a bean DTO at the API boundary.
 *
 * @author Arnaud Charleroy
 */
public record MongoVolumetry(
        String database,
        Long objectCount,
        Long collectionCount,
        Long dataSizeBytes,
        Long storageSizeBytes,
        Long indexSizeBytes,
        List<CollectionVolumetry> collections,
        Instant computedAt,
        String message) {

    public record CollectionVolumetry(
            String name,
            Long documentCount,
            Long sizeBytes,
            Long storageSizeBytes,
            Long indexSizeBytes) {
    }
}
