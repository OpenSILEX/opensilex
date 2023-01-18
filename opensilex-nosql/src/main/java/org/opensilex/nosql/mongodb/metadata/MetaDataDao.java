package org.opensilex.nosql.mongodb.metadata;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.arq.querybuilder.Order;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.opensilex.nosql.mongodb.MongoModel.MONGO_ID_FIELD;

/**
 * @author rcolin
 * Dao used to handle {@link MetaDataModel}
 * #TODO use this Dao for each CRUD method
 */
public class MetaDataDao {

    private final MongoDBService mongodb;

    public MetaDataDao(MongoDBService mongodb) {
        this.mongodb = mongodb;
    }

    /**
     * @param collection collection which contains {@link MetaDataModel}
     * @param idField    name of the field on which filter models
     * @param models     list of models. This method use models URIS for filtering and then update models
     * @param consumer   Consumer which allow the function caller to determine how to use model and MetaDataModel
     * @param <T>        Type of {@link SPARQLResourceModel} for which we want to set {@link MetaDataModel}
     */
    public <T extends SPARQLResourceModel> void getMetaDataAssociatedTo(MongoCollection<MetaDataModel> collection,
                                                                        String idField,
                                                                        Collection<T> models,
                                                                        BiConsumer<T, MetaDataModel> consumer) {

        FindIterable<MetaDataModel> attributesIt = mongodb.findIterableByURIs(
                idField,
                collection,
                models.stream().map(SPARQLResourceModel::getUri)
        );

        // use an index in order to map incoming mongodb results with input URIs
        // O(n) time/space complexity in local
        Map<String, MetaDataModel> index = new PatriciaTrie<>();
        for (MetaDataModel metaDataModel : attributesIt) {
            index.put(metaDataModel.getUri().toString(), metaDataModel);
        }

        for (T model : models) {
            MetaDataModel metadata = index.get(URIDeserializer.formatURIAsStr(model.getUri().toString()));

            if (metadata != null) {
                // use lambda in order to let setMetadata() caller determine how to use returned metadata
                consumer.accept(model, metadata);
            }
        }
    }

    /**
     * @param collection the collection of {@link MetaDataModel}
     * @param filter     an optional and additional filter on collection documents
     * @return each unique attribute key from the MetaDataModel collection
     */
    public Set<String> getDistinctKeys(MongoCollection<MetaDataModel> collection, Bson filter) {

        List<Bson> aggregatePipeline = new ArrayList<>();

        // 0. pre-filter of document according custom filter
        if (filter != null) {
            aggregatePipeline.add(Aggregates.match(filter));
        }

        // 1.project/computed - Transform document to multiple array elements
        // Each item in array has k (original document key) and v (original document value) fields
        final String computedArrayFieldName = "computed_attributes";
        Document objectToArrayAggregation = new Document("$objectToArray", "$" + MetaDataModel.ATTRIBUTES_FIELD);

        aggregatePipeline.add(Aggregates.project(
                Projections.computed(computedArrayFieldName, objectToArrayAggregation)
        ));

        // 2.unwind - Transform multiple array elements to simple array
        aggregatePipeline.add(Aggregates.unwind("$" + computedArrayFieldName));

        // 3.group - Return unique arrays keys only (using .k field)
        aggregatePipeline.add(Aggregates.group("$" + computedArrayFieldName + ".k"));

        // build a set which maintains natural order between each key
        Set<String> distinct = new TreeSet<>();

        // map aggregation results a Document, since the aggregation will produce a different document schema
        collection.aggregate(aggregatePipeline, Document.class).map(
                document -> document.getString(MONGO_ID_FIELD)  // _id field equals to the desired unique key
        ).forEach(distinct::add);

        return distinct;
    }

    /**
     * @param collection the collection of {@link MetaDataModel} (required)
     * @param attributeKey        the attribute key (required)
     * @param attributeValue    attribute search pattern (optional)
     * @param page       page number
     * @param pageSize   page size
     * @return each unique attribute values for the given attribute from the MetaDataModel collection
     * 
     * @see MongoDBService#distinctWithPagination(MongoCollection, String, Function, Bson, List, int, int)
     */
    public Set<String> getDistinctValues(MongoCollection<MetaDataModel> collection, String attributeKey, String attributeValue, int page, int pageSize) {

        String attributeFieldName = MetaDataModel.ATTRIBUTES_FIELD + "." + attributeKey;

        Bson filter;

        // no filter on value, retrieve all document which have the attribute key
        if (attributeValue == null) {
            filter = Filters.exists(attributeFieldName);
        } else { // filter on attribute + attribute value (with regex, case insensitive)
            filter = Filters.regex(attributeFieldName, attributeValue, "i");
        }

        // compute distinct attribute values
        // only document with the specified attributed defined, are integrated inside the aggregation pipeline
        // sort by ASC order on "_id". At this stage of the pipeline, "_id" is equal to attribute value
        return mongodb.distinctWithPagination(
                collection,
                attributeFieldName,
                document -> document.getString(MONGO_ID_FIELD),
                filter,
                Collections.singletonList(new OrderBy(MONGO_ID_FIELD, Order.ASCENDING)),
                page,
                pageSize
        );
    }

    /**
     * @param collection the collection of {@link MetaDataModel}
     * @param filterDocument   a Document which contains a filter
     * @return the set of URI of any {@link MetaDataModel} which match the given metadata filter
     * @apiNote This method read each (key,value) of metadata :
     * <ul>
     *     <li>If only key is set, then all document with the key defined are returned</li>
     *     <li>If key and value are set, then all document with the the good key and value are returned</li>
     *     <li>A Regex filter (case un-sensitive) is applied on value</li>
     * </ul>
     */
    public Set<URI> searchUris(MongoCollection<MetaDataModel> collection, Document filterDocument) {

        if (MapUtils.isEmpty(filterDocument)) {
            return Collections.emptySet();
        }

        List<Bson> filters = new LinkedList<>();

        // read each key,value and build a filter according pair
        filterDocument.forEach((key, value) -> {

            String attributeFieldName = MetaDataModel.ATTRIBUTES_FIELD + "." + key;

            // only retrieve document with an existing attributes."key" field defined
            if (value == null || value.toString().isEmpty()) {
                filters.add(Filters.exists(attributeFieldName));
            } else {
                // append regex filter on value
                filters.add(Filters.regex(attributeFieldName, value.toString(), "i"));
            }

        });

        Set<URI> distinctUris = new HashSet<>();

        // performs distinct query with filter
        collection.distinct(MongoModel.URI_FIELD, Filters.and(filters), URI.class)
                .forEach(distinctUris::add); // collect URIs inside a Set

        return distinctUris;
    }


}
