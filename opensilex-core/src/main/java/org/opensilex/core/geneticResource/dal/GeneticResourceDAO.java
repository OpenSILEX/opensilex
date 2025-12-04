//******************************************************************************
//                          GeneticResourceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.dal;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.bson.Document;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geneticResource.api.GeneticResourceSearchFilter;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.metadata.MetaDataDaoV2;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.nosql.mongodb.metadata.MetadataSearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;

/**
 * Data Access Object for GeneticResource, metadatas (also known as attributes) are stored in MongoDB while the rest is stored in RDF.
 * @author Alice Boizet
 */
public class GeneticResourceDAO {

    protected final SPARQLService sparql;
    protected final MongoDBServiceV2 nosql;
    private final GeneticResourceSparqlDAO sparqlDAO;
    private final MetaDataDaoV2 metaDataDao;
    public static final String ATTRIBUTES_COLLECTION_NAME = "geneticResourceAttribute";

    public GeneticResourceDAO(SPARQLService sparql, MongoDBServiceV2 nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.sparqlDAO = new GeneticResourceSparqlDAO(sparql);
        this.metaDataDao = new MetaDataDaoV2(nosql, ATTRIBUTES_COLLECTION_NAME);

        MongoCollection<MetaDataModel> collection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, MetaDataModel.class);
        collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
    }

    public GeneticResourceDAO(SPARQLService sparql, MongoDBService nosql) {
        this(sparql, nosql.getServiceV2());
    }

    public GeneticResourceModel update(GeneticResourceModel model, AccountModel user) throws Exception {
        //sparqlDAO.validateGeneticResourceAccess(model.getUri(), user);
        MetaDataModel storedAttributes = getStoredAttributes(model.getUri());
        MetaDataModel attributeModel = model.getMetadata();

        if (((attributeModel == null || MapUtils.isEmpty(attributeModel.getAttributes())) && storedAttributes == null)) {
            sparqlDAO.update(model,user);
        } else {
            new SparqlMongoTransaction(sparql, nosql).execute(session -> {
                sparqlDAO.update(model, user);

                if (attributeModel != null && !MapUtils.isEmpty(attributeModel.getAttributes())) {
                    attributeModel.setUri(model.getUri());
                    metaDataDao.upsert(session, attributeModel);
                } else {
                    metaDataDao.delete(session, model.getUri());
                }
                return null;
            });
        }
        return model;
    }

    public List<GeneticResourceModel> updateList(List<GeneticResourceModel> models) throws Exception {
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            sparql.update(models);
            this.upsertMetaData(models, session);
            return null;
        });
        return models;
    }

    /**
     * upsert metadata attributes for a list of geneticResources. Should be called in a transaction.
     * @param instanceList list of geneticResource models
     * @param session MongoDB client session
     */
    private void upsertMetaData(List<GeneticResourceModel> instanceList, ClientSession session) throws Exception {
        if (instanceList.isEmpty()) {
            return;
        }
        for (GeneticResourceModel model : instanceList) {
            MetaDataModel storedAttributes = getStoredAttributes(model.getUri());
            MetaDataModel metadata = model.getMetadata();

            if (((metadata == null || MapUtils.isEmpty(metadata.getAttributes())) && storedAttributes == null)) {
                return;
            }
            if (metadata != null && !MapUtils.isEmpty(metadata.getAttributes())) {
                model.getMetadata().setUri(model.getUri());
                metaDataDao.upsert(session, model.getMetadata());
            } else {
                metaDataDao.delete(session, model.getUri());
            }
        }
    }

    public GeneticResourceModel create(GeneticResourceModel model) throws Exception {
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            sparqlDAO.create(model);
            if(model.getMetadata() != null){
                //Set the metaDataModel's uri to be the same as the device
                model.getMetadata().setUri(model.getUri());

                metaDataDao.create(session, model.getMetadata());
            }
            return null;
        });
        return model;
    }

    /**
     * WARNING, you should check that each geneticResource of the list has a URI that doesn't exist in the database BEFORE calling this method.
     * This method create many geneticResources at a time without checking that their URI doesn't already exist to optimise database call.
     * @param instanceList with every model you need to create
     */
    public List<GeneticResourceModel> createListWithoutUriExistsCheck(List<GeneticResourceModel> instanceList) throws Exception {
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            sparql.create(sparql.getDefaultGraph(GeneticResourceModel.class), instanceList, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false, true);
            this.createMetaData(instanceList, session);
            return null;
        });
        return instanceList;
    }

    /**
     * create metadata attributes for a list of geneticResources. Should be called in a transaction.
     */
    private void createMetaData(List<GeneticResourceModel> instanceList, ClientSession session) throws Exception {
        if (instanceList.isEmpty()) {
            return;
        }
        for (GeneticResourceModel model : instanceList) {
            if (model.getMetadata() != null) {
                model.getMetadata().setUri(model.getUri());
                metaDataDao.create(session, model.getMetadata());
            }
        }
    }

    /**
     * Get a geneticResource by its URI
     * @param uri URI of the geneticResource
     * @param user User account
     * @param withNested boolean to indicate if nested objects should be fetched (parent geneticResources)
     * @return GeneticResourceModel
     */
    public GeneticResourceModel get(URI uri, AccountModel user, boolean withNested) throws Exception {
        //sparqlDAO.validateGeneticResourceAccess(uri, user);
        GeneticResourceModel geneticResource = sparqlDAO.get(uri, user, withNested);
        if (geneticResource != null) {
            MetaDataModel storedAttributes = getStoredAttributes(geneticResource.getUri());
            if (storedAttributes != null) {
                geneticResource.setMetadata(storedAttributes);
            }
        }
        return geneticResource;
    }

    /**
     * Paginated search of {@link GeneticResourceModel} according to the provided criteria, with an option
     * to filter by metadata and the ability to load nested objects.
     * <p>
     * If a metadata filter is provided, the corresponding URIs are extracted
     * and intersected with the selected URIs. If no results match, an
     * empty list is returned.
     * </p>
     *
     * @param searchFilter        search criteria (filters, pagination, sorting, access rights)
     * @param fetchMetadata       {@code true} to retrieve and associate metadata with the results
     * @param fetchNestedObjects  {@code true} to load nested relationships (e.g., parents)
     * @return a paginated list of {@link GeneticResourceModel} matching the criteria
     * @throws Exception if a SPARQL search or metadata retrieval error occurs
     */

    public ListWithPagination<GeneticResourceModel> search(
            GeneticResourceSearchFilter searchFilter,
            boolean fetchMetadata,
            boolean fetchNestedObjects) throws Exception {

        final Set<URI> filteredUris;
        if (searchFilter.getMetadata() != null) {
            MetadataSearchFilter metadataSearchFilter = new MetadataSearchFilter();
            metadataSearchFilter.setAttributes(Document.parse(searchFilter.getMetadata()));
            filteredUris = new HashSet<>(metaDataDao.distinctUris(metadataSearchFilter));

            // no URI match the given metadata filter, return empty list
            if (filteredUris.isEmpty()) {
                return new ListWithPagination<>(Collections.emptyList());
            }
        } else {
            filteredUris = new HashSet<>();
        }

        if (!CollectionUtils.isEmpty(searchFilter.getUris())) {
            if (filteredUris.isEmpty()) {
                // only use selected uris
                filteredUris.addAll(searchFilter.getUris());
            } else {
                // metadata URI filter + selected uris, use Set intersection
                filteredUris.retainAll(searchFilter.getUris());
            }
        }

        searchFilter.setUris(new ArrayList<>(filteredUris));

        ListWithPagination<GeneticResourceModel> models = sparqlDAO.search(
                searchFilter,
                fetchNestedObjects
        );

        if (fetchMetadata) {
            // get all GeneticResource metadata with one query
            metaDataDao.getMetaDataAssociatedTo(
                    models.getList(), // get Metadata associated with GeneticResource uris
                    GeneticResourceModel::setMetadata // update GeneticResource metadata
            );
        }
        return models;
    }

    public int countInGroup(URI group, String lang) throws Exception {
        return sparqlDAO.countInGroup(group, lang);
    }

    public boolean isGeneticResourceType(URI rdfType) throws SPARQLException {
        return sparqlDAO.isGeneticResourceType(rdfType);
    }

    public void delete(URI uri, AccountModel user) throws Exception {

        //sparqlDAO.validateGeneticResourceAccess(uri, user);
        new SparqlMongoTransaction(sparql, nosql).execute(session -> {
            if (metaDataDao.exists(uri)) {
                metaDataDao.delete(session, uri);
            }
            sparqlDAO.delete(uri,user);
            return null;
        });
    }

    public boolean isLinkedToSth(GeneticResourceModel geneticResource) throws SPARQLException {
        return sparqlDAO.isLinkedToSth(geneticResource);
    }

    /**
     * Get all geneticResource attributes
     */
    public Set<String> getDistinctGeneticResourceAttributes() {
        return metaDataDao.getDistinctKeys();
    }

    public Set<String> getDistinctGeneticResourceAttributesValues(String attribute, String attributeValue, int page, int pageSize) {
        return metaDataDao.getDistinctValues(attribute,attributeValue,page,pageSize);
    }

    /**
     * Get all experiments associated with a geneticResource
     */
    // @Todo: put this method in the experiment package ? It's an experiment fetcher with a geneticResource filter so it's not really a geneticResource method. - Yvan 06/06/2024
    public ListWithPagination<ExperimentModel> getExpFromGeneticResource(
            AccountModel currentUser,
            URI uri,
            String name,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return sparqlDAO.getExpFromGeneticResource(currentUser, uri, name, orderByList, page, pageSize);

    }

    private MetaDataModel getStoredAttributes(URI uri) {
        MetaDataModel storedAttributes = null;
        try {
            storedAttributes = metaDataDao.get(uri);
        } catch (NoSQLInvalidURIException ignored) {
        }
        return storedAttributes;
    }

    public ListWithPagination<GeneticResourceModel> brapiSearch(AccountModel user, URI geneticResourceDbId, String geneticResourceName, String geneticResourceSpecies, int page, int pageSize) throws Exception {
        return sparqlDAO.brapiSearch(user, geneticResourceDbId, geneticResourceName, geneticResourceSpecies, page, pageSize);
    }

    /**
     * @return the list of existing URIs in geneticResource graph among the given list of URIs
     */
    public Collection<URI> checkExistence(List<URI> uris) throws Exception {
        return sparqlDAO.sparql.getExistingUris(GeneticResourceModel.class, uris, true);
    }
}
