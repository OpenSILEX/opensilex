//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health.stats;

import com.mongodb.MongoCommandException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.opensilex.monitoring.config.HealthConfig;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads MongoDB volumetry through {@code dbStats} and {@code $collStats}.
 *
 * <p>Every failure degrades to a null figure plus a message. A locked-down deployment can deny
 * these commands, and a monitoring page that returns 500 because it cannot measure something is
 * worse than one that says so.</p>
 *
 * @author Arnaud Charleroy
 */
public class MongoStatsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoStatsReader.class);

    private final MongoDBServiceV2 mongodb;
    private final HealthConfig config;

    public MongoStatsReader(MongoDBServiceV2 mongodb, HealthConfig config) {
        this.mongodb = mongodb;
        this.config = config;
    }

    public MongoVolumetry read() {
        String databaseName = mongodb.getImplementedConfig().database();
        if (!config.mongoDatabaseStats()) {
            return new MongoVolumetry(databaseName, null, null, null, null, null,
                    List.of(), Instant.now(), "database statistics disabled by configuration");
        }

        try {
            Document stats = mongodb.getDatabase()
                    .runCommand(new Document("dbStats", 1).append("scale", 1));

            return new MongoVolumetry(
                    databaseName,
                    asLong(stats.get("objects")),
                    asLong(stats.get("collections")),
                    asLong(stats.get("dataSize")),
                    asLong(stats.get("storageSize")),
                    asLong(stats.get("indexSize")),
                    config.mongoCollectionStats() ? readCollections() : List.of(),
                    Instant.now(),
                    null);
        } catch (MongoCommandException e) {
            LOGGER.warn("Cannot read MongoDB database statistics: {}", e.getErrorMessage());
            return new MongoVolumetry(databaseName, null, null, null, null, null,
                    List.of(), Instant.now(), "dbStats refused: " + e.getErrorMessage());
        } catch (Exception e) {
            LOGGER.warn("Cannot read MongoDB database statistics", e);
            return new MongoVolumetry(databaseName, null, null, null, null, null,
                    List.of(), Instant.now(), e.getClass().getSimpleName());
        }
    }

    private List<MongoVolumetry.CollectionVolumetry> readCollections() {
        List<MongoVolumetry.CollectionVolumetry> result = new ArrayList<>();
        try {
            for (String name : mongodb.getDatabase().listCollectionNames()) {
                try {
                    // The aggregation stage rather than the collStats command, which is deprecated.
                    Document row = mongodb.getDatabase().getCollection(name)
                            .aggregate(List.of(new Document("$collStats",
                                    new Document("storageStats", new Document("scale", 1)))),
                                    Document.class)
                            .first();
                    Document storage = row == null ? null : row.get("storageStats", Document.class);
                    if (storage != null) {
                        result.add(new MongoVolumetry.CollectionVolumetry(
                                name,
                                asLong(storage.get("count")),
                                asLong(storage.get("size")),
                                asLong(storage.get("storageSize")),
                                asLong(storage.get("totalIndexSize"))));
                    }
                } catch (Exception perCollection) {
                    LOGGER.debug("Cannot read statistics for collection {}", name, perCollection);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot list MongoDB collections", e);
        }
        result.sort((a, b) -> Long.compare(
                b.storageSizeBytes() == null ? 0L : b.storageSizeBytes(),
                a.storageSizeBytes() == null ? 0L : a.storageSizeBytes()));
        return result;
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
