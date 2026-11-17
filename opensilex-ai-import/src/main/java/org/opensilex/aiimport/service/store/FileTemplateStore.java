//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.nosql.mongodb.MongoDBService;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Where the templates a user keeps are kept: their own, and those others shared. A template has no
 * expiry — it is what a user built up to import the next file faster, and only its author deletes it.
 * <p>
 * Held in the platform's document database as the conversations are: a few searchable fields beside
 * a JSON snapshot.
 *
 * @author Arnaud Charleroy
 */
public class FileTemplateStore {

    public static final String COLLECTION = "ai_import_templates";

    static final String ID = "_id";
    static final String AUTHOR = "author";
    static final String SHARED = "shared";
    static final String UPDATED_AT = "updated_at";
    static final String SNAPSHOT = "snapshot";

    private static volatile boolean indexed;

    private final MongoDBService nosql;
    private final ObjectMapper mapper;

    public FileTemplateStore(MongoDBService nosql, ObjectMapper mapper) {
        this.nosql = nosql;
        this.mapper = mapper;
    }

    /**
     * Stores a template, replacing its previous state.
     */
    public void save(FileTemplate template) throws IOException {
        Document document = new Document(ID, template.getId())
                .append(AUTHOR, template.getAuthor().toString())
                .append(SHARED, template.isShared())
                .append(UPDATED_AT, Date.from(template.getUpdatedAt()))
                .append(SNAPSHOT, mapper.writeValueAsString(template));
        collection().replaceOne(Filters.eq(ID, template.getId()), document, new ReplaceOptions().upsert(true));
    }

    /**
     * @return the template when this user may read it: theirs, or shared. Someone else's private
     * template is indistinguishable from one that does not exist.
     */
    public Optional<FileTemplate> find(String id, URI user) throws IOException {
        Document document = collection().find(Filters.and(Filters.eq(ID, id), visibleTo(user))).first();
        return document == null ? Optional.empty() : Optional.of(read(document));
    }

    /**
     * @return the templates this user may read, their own and those shared, most recently changed first
     */
    public List<FileTemplate> list(URI user) throws IOException {
        List<FileTemplate> templates = new ArrayList<>();
        for (Document document : collection().find(visibleTo(user)).sort(Sorts.descending(UPDATED_AT))) {
            templates.add(read(document));
        }
        return templates;
    }

    /**
     * @return whether a template of this author was deleted
     */
    public boolean delete(String id, URI author) {
        return collection().deleteOne(Filters.and(Filters.eq(ID, id), Filters.eq(AUTHOR, author.toString())))
                .getDeletedCount() > 0;
    }

    private FileTemplate read(Document document) throws IOException {
        return mapper.readValue(document.getString(SNAPSHOT), FileTemplate.class);
    }

    private static Bson visibleTo(URI user) {
        return Filters.or(Filters.eq(AUTHOR, user.toString()), Filters.eq(SHARED, true));
    }

    private MongoCollection<Document> collection() {
        MongoCollection<Document> collection = nosql.getDatabase().getCollection(COLLECTION);
        if (!indexed) {
            synchronized (FileTemplateStore.class) {
                if (!indexed) {
                    collection.createIndex(Indexes.compoundIndex(Indexes.ascending(AUTHOR),
                            Indexes.descending(UPDATED_AT)));
                    collection.createIndex(Indexes.ascending(SHARED));
                    indexed = true;
                }
            }
        }
        return collection;
    }
}
