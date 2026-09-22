//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.opensilex.monitoring.extension.HealthIndicator;
import org.opensilex.monitoring.extension.ProbeResult;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

/**
 * MongoDB liveness, using the very same ping command the driver service runs at startup.
 *
 * @author Arnaud Charleroy
 */
public class MongoHealthIndicator implements HealthIndicator {

    public static final String NAME = "mongodb";

    private final MongoDBServiceV2 mongodb;

    public MongoHealthIndicator(MongoDBServiceV2 mongodb) {
        this.mongodb = mongodb;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ProbeResult probe() {
        long start = System.nanoTime();
        mongodb.getDatabase().runCommand(new BsonDocument("ping", new BsonInt32(1)));
        return ProbeResult.up(elapsedMs(start));
    }

    static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
