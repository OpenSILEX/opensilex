//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log.dal;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access to the {@code monitoringRequestLog} collection.
 *
 * <p>The collection name is deliberately not {@code log}: the core module owns that one through
 * {@code LogsDAO} and must stay untouched.</p>
 *
 * @author Arnaud Charleroy
 */
public class RequestLogDao extends MongoReadWriteDao<RequestLogModel, RequestLogSearchFilter> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogDao.class);

    public static final String COLLECTION_NAME = "monitoringRequestLog";
    public static final String PREFIX = "monitoringRequestLog";
    public static final String TTL_INDEX_NAME = "monitoringRequestLog_startTime_ttl";

    private static final int INDEX_OPTIONS_CONFLICT = 85;

    public RequestLogDao(MongoDBServiceV2 mongodb) {
        super(mongodb, RequestLogModel.class, COLLECTION_NAME, PREFIX);
    }

    /**
     * Non-TTL indexes only.
     *
     * <p>There is deliberately no standalone index on {@code startTime}: the TTL index already is
     * {@code {startTime: 1}}, MongoDB walks it backwards for a descending sort, and a second one
     * would be pure write amplification on a write-heavy collection — as well as a guaranteed
     * options conflict with the TTL index it duplicates.</p>
     */
    public static Map<Bson, IndexOptions> getIndexes() {
        Map<Bson, IndexOptions> indexes = new HashMap<>();
        indexes.put(Indexes.compoundIndex(
                Indexes.ascending(RequestLogModel.ACCOUNT_FIELD),
                Indexes.descending(RequestLogModel.START_TIME_FIELD)), null);
        indexes.put(Indexes.compoundIndex(
                Indexes.ascending(RequestLogModel.SUCCESS_FIELD),
                Indexes.descending(RequestLogModel.START_TIME_FIELD)), null);
        return indexes;
    }

    /**
     * Creates or realigns the TTL index, and never prevents the instance from starting.
     *
     * <p>This cannot go through {@code MongoDBServiceV2.registerIndexes}. That path calls
     * {@code createIndex}, which swallows error code 86 ({@code IndexKeySpecsConflict}) but not 85
     * ({@code IndexOptionsConflict}) — and 85 is exactly what MongoDB raises when the index already
     * exists with a different {@code expireAfterSeconds}. Changing {@code retentionDays} in the
     * configuration would therefore abort module startup.</p>
     *
     * <p>{@code collMod} is the right primitive for the realignment: it changes the expiry in place
     * with no index rebuild. Dropping and recreating is only the fallback, because it leaves a
     * window with no purge and rebuilds an index over a potentially large collection.</p>
     */
    public static void ensureTtlIndex(MongoDBServiceV2 mongodb, int retentionDays) {
        try {
            MongoCollection<Document> collection =
                    mongodb.getDatabase().getCollection(COLLECTION_NAME);

            if (retentionDays <= 0) {
                LOGGER.warn("monitoring: requestLog.retentionDays is {}, access log entries will"
                        + " never expire", retentionDays);
                dropTtlIndexIfPresent(collection);
                return;
            }

            long expireAfterSeconds = (long) retentionDays * 86_400L;
            Document existing = findTtlIndex(collection);

            if (existing == null) {
                collection.createIndex(Indexes.ascending(RequestLogModel.START_TIME_FIELD),
                        ttlOptions(expireAfterSeconds));
                LOGGER.info("monitoring: created TTL index on {} ({} days)",
                        COLLECTION_NAME, retentionDays);
                return;
            }

            Number current = existing.get("expireAfterSeconds", Number.class);
            if (current != null && current.longValue() == expireAfterSeconds) {
                return;
            }

            try {
                mongodb.getDatabase().runCommand(new Document("collMod", COLLECTION_NAME)
                        .append("index", new Document("name", TTL_INDEX_NAME)
                                .append("expireAfterSeconds", expireAfterSeconds)));
                LOGGER.info("monitoring: TTL index on {} realigned to {} days",
                        COLLECTION_NAME, retentionDays);
            } catch (MongoCommandException e) {
                LOGGER.warn("monitoring: collMod refused on {} ({}), falling back to a rebuild",
                        COLLECTION_NAME, e.getErrorMessage());
                collection.dropIndex(TTL_INDEX_NAME);
                collection.createIndex(Indexes.ascending(RequestLogModel.START_TIME_FIELD),
                        ttlOptions(expireAfterSeconds));
            }
        } catch (Exception e) {
            // Monitoring must never keep the instance from booting.
            LOGGER.warn("monitoring: could not ensure the access log TTL index", e);
        }
    }

    private static void dropTtlIndexIfPresent(MongoCollection<Document> collection) {
        try {
            if (findTtlIndex(collection) != null) {
                collection.dropIndex(TTL_INDEX_NAME);
            }
        } catch (Exception e) {
            LOGGER.debug("monitoring: could not drop the TTL index", e);
        }
    }

    private static Document findTtlIndex(MongoCollection<Document> collection) {
        for (Document index : collection.listIndexes()) {
            if (TTL_INDEX_NAME.equals(index.getString("name"))) {
                return index;
            }
        }
        return null;
    }

    private static IndexOptions ttlOptions(long expireAfterSeconds) {
        return new IndexOptions()
                .name(TTL_INDEX_NAME)
                .expireAfter(expireAfterSeconds, TimeUnit.SECONDS)
                .background(true);
    }

    @Override
    public List<Bson> getBsonFilters(RequestLogSearchFilter filter) {
        List<Bson> result = super.getBsonFilters(filter);

        if (filter.getStartDate() != null) {
            result.add(Filters.gte(RequestLogModel.START_TIME_FIELD, filter.getStartDate()));
        }
        if (filter.getEndDate() != null) {
            result.add(Filters.lt(RequestLogModel.START_TIME_FIELD, filter.getEndDate()));
        }
        if (filter.getAccount() != null) {
            result.add(Filters.eq(RequestLogModel.ACCOUNT_FIELD, filter.getAccount()));
        }
        if (filter.getSuccess() != null) {
            result.add(Filters.eq(RequestLogModel.SUCCESS_FIELD, filter.getSuccess()));
        }
        if (filter.getHttpMethod() != null && !filter.getHttpMethod().isBlank()) {
            result.add(Filters.eq(RequestLogModel.HTTP_METHOD_FIELD, filter.getHttpMethod()));
        }
        if (filter.getPathPrefix() != null && !filter.getPathPrefix().isBlank()) {
            result.add(Filters.regex(RequestLogModel.PATH_FIELD,
                    "^" + Pattern.quote(filter.getPathPrefix())));
        }
        return result;
    }
}
