package org.opensilex.nosql.mongodb.metadata;

import com.mongodb.MongoException;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;

import static org.opensilex.nosql.mongodb.MongoModel.MONGO_ID_FIELD;

/**
 * @author mhart
 * Dao used to handle {@link MetaDataModel}
 */
public class MetaDataDaoV2 extends MongoReadWriteDao<MetaDataModel, MetadataSearchFilter> {

    public MetaDataDaoV2(MongoDBServiceV2 mongodb, String collectionName) {
        super(mongodb, MetaDataModel.class, collectionName, collectionName);
    }

    public MetaDataDaoV2(MongoDBService mongodb, String collectionName) {
        this(mongodb.getServiceV2(), collectionName);
    }

    public static Map<Bson, IndexOptions> getIndexes() {
        Map<Bson, IndexOptions> indexes = new HashMap<>();
        indexes.put(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
        return indexes;
    }

    @Override
    public List<Bson> getBsonFilters(MetadataSearchFilter searchQuery) {
        List<Bson> result = super.getBsonFilters(searchQuery);

        if(searchQuery.getAttributes() != null){
            for (String key : searchQuery.getAttributes().keySet()) {
                result.add(Filters.eq("attributes." + key, searchQuery.getAttributes().get(key)));
            }
        }
        return result;
    }

    /**
     * @param filter is used to filter metadata models, a field with no value or "" juste has to be present in the metadata model,
     *              a field with a value will be checked with a regex
     * @return uris of metadata models corresponding to the filter
     */
    @Override
    public List<URI> distinctUris(MetadataSearchFilter filter) throws MongoException {
        Document attributes = filter.getAttributes();
        attributes.forEach((key, value) -> {
            if (value == null || value.toString().isEmpty()) {
                attributes.put(key, new Document("$exists", true));
            } else {
                attributes.put(key, getRegex(value));
            }
        });
        MetadataSearchFilter metaDataSearchFilter = new MetadataSearchFilter();
        metaDataSearchFilter.setAttributes(attributes);
        return super.distinctUris(filter);
    }

    /**
     * Retrieve MetaDataModel associated to a list of models and consume them with a BiConsumer
     * @param models     list of models. This method use models URIS for filtering and then update models
     * @param consumer   Consumer which allow the function caller to determine how to use model and MetaDataModel
     * @param <T>        Type of {@link SPARQLResourceModel} for which we want to set {@link MetaDataModel}
     */
    public <T extends SPARQLResourceModel> void getMetaDataAssociatedTo(Collection<T> models,
                                                                        BiConsumer<T, MetaDataModel> consumer) {

        // store metadataModels by their URi to easily associate them with the model having the same URI
        Map<String, MetaDataModel> metadatas = new HashMap<>();

        findByUris(models.stream().
                map(SPARQLResourceModel::getUri), models.size()
        ).forEach(metadata -> metadatas.put(SPARQLDeserializers.getShortURI(metadata.getUri()), metadata));

        models.forEach(model -> {
            MetaDataModel metadata = metadatas.get(SPARQLDeserializers.getShortURI(model.getUri()));
            if (metadata != null) {
                consumer.accept(model, metadata);
            }
        });
    }

    /**
     * @return each unique attribute key from the MetaDataModel collection
     */
    public Set<String> getDistinctKeys() {

        List<Bson> aggregatePipeline = new ArrayList<>();

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
     * @param attributeKey        the attribute key (required)
     * @param attributeValue    attribute search pattern (optional)
     * @param page       page number
     * @param pageSize   page size
     * @return each unique attribute values for the given attribute from the MetaDataModel collection
     *
     * @see MongoDBService#distinct(String, Class, String, Document)
     */
    public Set<String> getDistinctValues(String attributeKey, String attributeValue, int page, int pageSize) {

        String attributeFieldName = MetaDataModel.ATTRIBUTES_FIELD + "." + attributeKey;

        MetadataSearchFilter filter = new MetadataSearchFilter();
        filter.setPage(page);
        filter.setPageSize(pageSize);

        // no filter on value, retrieve all document which have the attribute key
        if (attributeValue == null) {
            filter.setAttributes(new Document(attributeKey, new Document("$exists", "true")));
        } else { // filter on attribute + attribute value
            filter.setAttributes(new Document(attributeKey, getRegex(attributeValue)));
        }

        return new HashSet<>(distinct(
                null,
                attributeFieldName,
                String.class,
                filter
        ));
    }

    //#region private methods

    /**
     * @return document with regex and case-insensitive options : { "$regex": value, "$options": "i" }
     */
    private Document getRegex(Object value) {
        return new Document("$regex", value).append("$options", "i");
    }
    //#endregion
}
