//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.alias;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.nosql.mongodb.MongoDBService;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * The aliases each user taught the assistant, in MongoDB: for a header and a class, the property the
 * user chose, and how many times.
 * <p>
 * Kept per user, so that one user's correction never decides another user's column.
 *
 * @author Arnaud Charleroy
 */
public class LearnedAliasStore implements LearnedAliases {

    public static final String COLLECTION = "graph_import_aliases";

    private static final String OWNER = "owner";
    private static final String CLASS = "class";
    private static final String HEADER = "header";
    private static final String PROPERTY = "property";
    private static final String COUNT = "count";
    private static final String UPDATED_AT = "updated_at";

    private static volatile boolean indexed;

    private final MongoDBService nosql;

    public LearnedAliasStore(MongoDBService nosql) {
        this.nosql = nosql;
    }

    @Override
    public Optional<URI> find(URI owner, URI classUri, String header) {
        Document best = collection()
                .find(Filters.and(Filters.eq(OWNER, owner.toString()), Filters.eq(CLASS, classUri.toString()),
                        Filters.eq(HEADER, header)))
                .sort(Sorts.orderBy(Sorts.descending(COUNT), Sorts.descending(UPDATED_AT)))
                .first();
        return best == null ? Optional.empty() : Optional.of(URI.create(best.getString(PROPERTY)));
    }

    /**
     * Records that the user chose this property for this header and class.
     *
     * @param header the header as written; stored in {@code HeaderMatcher.normalize} form
     */
    public void learn(URI owner, URI classUri, String header, URI property) {
        String normalized = HeaderMatcher.normalize(header);
        if (normalized.isEmpty()) {
            return;
        }
        Bson alias = Filters.and(Filters.eq(OWNER, owner.toString()), Filters.eq(CLASS, classUri.toString()),
                Filters.eq(HEADER, normalized), Filters.eq(PROPERTY, property.toString()));
        collection().updateOne(alias,
                Updates.combine(Updates.inc(COUNT, 1), Updates.set(UPDATED_AT, Date.from(Instant.now()))),
                new UpdateOptions().upsert(true));
    }

    private MongoCollection<Document> collection() {
        MongoCollection<Document> collection = nosql.getDatabase().getCollection(COLLECTION);
        if (!indexed) {
            collection.createIndex(Indexes.ascending(OWNER, CLASS, HEADER));
            indexed = true;
        }
        return collection;
    }
}
