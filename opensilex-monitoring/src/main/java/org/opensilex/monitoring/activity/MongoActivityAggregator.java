//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.activity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.monitoring.log.dal.RequestLogDao;
import org.opensilex.monitoring.log.dal.RequestLogModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB implementation of the activity aggregation.
 *
 * <p>Requires MongoDB 5.0 or later for {@code $dateTrunc}. The development stack runs 7.0 and the
 * embedded test server runs 7.0, so the constraint is met; it is documented because an older
 * production instance would fail with an unrecognised-expression error.</p>
 *
 * @author Arnaud Charleroy
 */
public class MongoActivityAggregator implements ActivityAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoActivityAggregator.class);

    private final RequestLogDao dao;
    private final String timezone;

    public MongoActivityAggregator(MongoDBServiceV2 mongodb, RequestLogDao dao) {
        this.dao = dao;
        String configured = mongodb.getImplementedConfig().timezone();
        // "Per day" has to mean the instance's day, not UTC's.
        this.timezone = configured == null || configured.isBlank() ? "UTC" : configured;
    }

    @Override
    public List<ActivityBucket> aggregate(Instant from, Instant to, Granularity granularity) {
        Document truncated = new Document("$dateTrunc", new Document("date", "$" + RequestLogModel.START_TIME_FIELD)
                .append("unit", granularity.getMongoUnit())
                .append("timezone", timezone));

        List<Bson> pipeline = List.of(
                new Document("$match", new Document(RequestLogModel.START_TIME_FIELD,
                        new Document("$gte", from).append("$lt", to))),

                // Stage one: one document per (bucket, account). Counting these documents in stage
                // two is what yields distinct users. $addToSet would be the obvious alternative,
                // but it builds one unbounded array of every distinct user per bucket, which on a
                // busy day can push the group document past the 16 MB BSON limit.
                new Document("$group", new Document("_id",
                        new Document("bucket", truncated)
                                .append("account", "$" + RequestLogModel.ACCOUNT_FIELD))
                        .append("requests", new Document("$sum", 1))
                        .append("clientErrors", new Document("$sum", errorCount(400, 500)))
                        .append("serverErrors", new Document("$sum", errorCount(500, 600)))),

                new Document("$group", new Document("_id", "$_id.bucket")
                        // Anonymous calls still count as requests and errors, but not as users.
                        .append("users", new Document("$sum", new Document("$cond",
                                List.of(new Document("$ifNull", List.of("$_id.account", false)), 1, 0))))
                        .append("requests", new Document("$sum", "$requests"))
                        .append("clientErrors", new Document("$sum", "$clientErrors"))
                        .append("serverErrors", new Document("$sum", "$serverErrors"))),

                new Document("$sort", new Document("_id", 1)));

        List<ActivityBucket> result = new ArrayList<>();
        for (Document row : dao.aggregate(pipeline, Document.class)) {
            Instant bucket = toInstant(row.get("_id"));
            if (bucket == null) {
                continue;
            }
            result.add(new ActivityBucket(
                    bucket,
                    asLong(row.get("users")),
                    asLong(row.get("requests")),
                    asLong(row.get("clientErrors")),
                    asLong(row.get("serverErrors"))));
        }
        return result;
    }

    @Override
    public long countDistinctUsers(Instant from, Instant to) {
        List<Bson> pipeline = List.of(
                new Document("$match", new Document(RequestLogModel.START_TIME_FIELD,
                        new Document("$gte", from).append("$lt", to))
                        .append(RequestLogModel.ACCOUNT_FIELD, new Document("$ne", null))),
                new Document("$group", new Document("_id", "$" + RequestLogModel.ACCOUNT_FIELD)),
                new Document("$count", "total"));

        for (Document row : dao.aggregate(pipeline, Document.class)) {
            return asLong(row.get("total"));
        }
        return 0L;
    }

    /**
     * Counts the calls whose status falls in {@code [lowerBound, upperBound)}. A call that ended in
     * an unhandled exception has no status of its own, so it is folded into the server errors.
     *
     * <p>The null-status comparison uses {@link Arrays#asList} and not {@code List.of}: the latter
     * rejects null elements outright, which threw a {@link NullPointerException} on every single
     * call to this endpoint.</p>
     */
    static Document errorCount(int lowerBound, int upperBound) {
        String status = "$" + RequestLogModel.STATUS_FIELD;
        Document inRange = new Document("$and", List.of(
                new Document("$gte", List.of(status, lowerBound)),
                new Document("$lt", List.of(status, upperBound))));
        Document condition = upperBound > 500
                ? new Document("$or", List.of(inRange,
                        new Document("$eq", Arrays.asList(status, null))))
                : inRange;
        return new Document("$cond", List.of(condition, 1, 0));
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        LOGGER.debug("Unexpected bucket key type: {}", value == null ? "null" : value.getClass());
        return null;
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
