//******************************************************************************
//                          GermplasmDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.bson.Document;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.nosql.mongodb.metadata.MetadataSearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Access Object for Germplasm, metadatas (also known as attributes) are stored in MongoDB while the rest is stored in RDF.
 * @author Alice Boizet
 */
public class GermplasmDAO {

    protected final SPARQLService sparql;
    protected final MongoDBServiceV2 nosql;
    private final GermplasmSparqlDAO sparqlDAO;
    private final GermplasmMetadataDAO metaDataDao;
    public static final String ATTRIBUTES_COLLECTION_NAME = "germplasmAttribute";

    public GermplasmDAO(SPARQLService sparql, MongoDBServiceV2 nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.sparqlDAO = new GermplasmSparqlDAO(sparql);
        this.metaDataDao = new GermplasmMetadataDAO(nosql, ATTRIBUTES_COLLECTION_NAME);

        MongoCollection<MetaDataModel> collection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, MetaDataModel.class);
        collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
    }

    public GermplasmDAO(SPARQLService sparql, MongoDBService nosql) {
        this(sparql, nosql.getServiceV2());
    }

    public GermplasmModel update(GermplasmModel model, AccountModel user) throws Exception {
        //sparqlDAO.validateGermplasmAccess(model.getUri(), user);
        var storedAttributes = getStoredAttributes(model.getUri());
        var attributeModel = model.getMetadata();

        if (((attributeModel == null || MapUtils.isEmpty(attributeModel.getAttributes())) && storedAttributes == null)) {
            sparqlDAO.update(model,user);
        } else {
            new SparqlMongoTransaction(sparql, nosql).execute(session -> {
                sparqlDAO.update(model, user);

                if (attributeModel != null && !MapUtils.isEmpty(attributeModel.getAttributes())) {
                    var modelWithPublisher = sparql.getByURI(GermplasmModel.class, model.getUri(), null);
                    updateMetadataFromGermplasm(attributeModel, modelWithPublisher);
                    metaDataDao.upsert(session, attributeModel);
                } else {
                    metaDataDao.delete(session, model.getUri());
                }
                return null;
            });
        }
        return model;
    }

    public List<GermplasmModel> updateList(List<GermplasmModel> models) throws Exception {
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            sparql.update(models);
            this.upsertMetaData(models, session);
            return null;
        });
        return models;
    }

    /**
     * upsert metadata attributes for a list of germplasms. Should be called in a transaction.
     * @param instanceList list of germplasm models
     * @param session MongoDB client session
     */
    private void upsertMetaData(List<GermplasmModel> instanceList, ClientSession session) throws Exception {
        if (instanceList.isEmpty()) {
            return;
        }
        for (GermplasmModel model : instanceList) {
            MetaDataModel storedAttributes = getStoredAttributes(model.getUri());
            MetaDataModel metadata = model.getMetadata();

            if (((metadata == null || MapUtils.isEmpty(metadata.getAttributes())) && storedAttributes == null)) {
                return;
            }
            if (metadata != null && !MapUtils.isEmpty(metadata.getAttributes())) {
                var modelWithPublisher = sparql.getByURI(GermplasmModel.class, model.getUri(), null);
                updateMetadataFromGermplasm(model.getMetadata(), modelWithPublisher);
                metaDataDao.upsert(session, model.getMetadata());
            } else {
                metaDataDao.delete(session, model.getUri());
            }
        }
    }

    public GermplasmModel create(GermplasmModel model) throws Exception {
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            sparqlDAO.create(model);
            if(model.getMetadata() != null){
                updateMetadataFromGermplasm(model.getMetadata(), model);
                metaDataDao.create(session, model.getMetadata());
            }
            return null;
        });
        return model;
    }

    /**
     * WARNING, you should check that each germplasm of the list has a URI that doesn't exist in the database BEFORE calling this method.
     * This method create many germplasms at a time without checking that their URI doesn't already exist to optimise database call.
     * @param instanceList with every model you need to create
     */
    public List<GermplasmModel> createListWithoutUriExistsCheck(List<GermplasmModel> instanceList) throws Exception {
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            sparql.create(sparql.getDefaultGraph(GermplasmModel.class), instanceList, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false, true);
            this.createMetaData(instanceList, session);
            return null;
        });
        return instanceList;
    }

    /**
     * create metadata attributes for a list of germplasms. Should be called in a transaction.
     */
    private void createMetaData(List<GermplasmModel> instanceList, ClientSession session) throws Exception {
        if (instanceList.isEmpty()) {
            return;
        }
        for (GermplasmModel model : instanceList) {
            if (model.getMetadata() != null) {
                updateMetadataFromGermplasm(model.getMetadata(), model);
                metaDataDao.create(session, model.getMetadata());
            }
        }
    }

    private void updateMetadataFromGermplasm(GermplasmMetadataModel metadata, GermplasmModel germplasm) {
        if (germplasm == null) {
            return;
        }
        metadata.setUri(germplasm.getUri());
        metadata.setPublisher(germplasm.getPublisher());
        metadata.setIsPublic(germplasm.getIsPublic());
        metadata.setGroups(germplasm.getGroups().stream().map(SPARQLResourceModel::getUri).toList());
    }

    /**
     * Get a germplasm by its URI
     * @param uri URI of the germplasm
     * @param user User account
     * @param withNested boolean to indicate if nested objects should be fetched (parent germplasms)
     * @return GermplasmModel
     */
    public GermplasmModel get(URI uri, AccountModel user, boolean withNested) throws Exception {
        //sparqlDAO.validateGermplasmAccess(uri, user);
        GermplasmModel germplasm = sparqlDAO.get(uri, user, withNested);
        if (germplasm != null) {
            var storedAttributes = getStoredAttributes(germplasm.getUri());
            if (storedAttributes != null) {
                germplasm.setMetadata(storedAttributes);
            }
        }
        return germplasm;
    }

    /**
     * Paginated search of {@link GermplasmModel} according to the provided criteria, with an option
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
     * @return a paginated list of {@link GermplasmModel} matching the criteria
     * @throws Exception if a SPARQL search or metadata retrieval error occurs
     */

    public ListWithPagination<GermplasmModel> search(
            GermplasmSearchFilter searchFilter,
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

        ListWithPagination<GermplasmModel> models = sparqlDAO.search(
                searchFilter,
                fetchNestedObjects
        );

        if (fetchMetadata) {
            // get all Germplasm metadata with one query
            metaDataDao.getMetaDataAssociatedTo(
                    models.getList(), // get Metadata associated with Germplasm uris
                    GermplasmModel::setMetadata // update Germplasm metadata
            );
        }
        return models;
    }

    public int countInGroup(URI group, String lang) throws Exception {
        return sparqlDAO.countInGroup(group, lang);
    }

    public boolean isGermplasmType(URI rdfType) throws SPARQLException {
        return sparqlDAO.isGermplasmType(rdfType);
    }

    public void delete(URI uri, AccountModel user) throws Exception {

        //sparqlDAO.validateGermplasmAccess(uri, user);
        new SparqlMongoTransaction(sparql, nosql).execute(session -> {
            if (metaDataDao.exists(uri)) {
                metaDataDao.delete(session, uri);
            }
            sparqlDAO.delete(uri,user);
            return null;
        });
    }

    public boolean isLinkedToSth(GermplasmModel germplasm) throws SPARQLException {
        return sparqlDAO.isLinkedToSth(germplasm);
    }

    /**
     * Get all germplasm attributes
     */
    public Set<String> getDistinctGermplasmAttributes(AccountModel user, List<GroupModel> userGroups) {
        return metaDataDao.getDistinctKeys(user, userGroups);
    }

    public Set<String> getDistinctGermplasmAttributesValues(String attribute, String attributeValue, int page, int pageSize) {
        return metaDataDao.getDistinctValues(attribute,attributeValue,page,pageSize);
    }

    /**
     * Get all experiments associated with a germplasm
     */
    // @Todo: put this method in the experiment package ? It's an experiment fetcher with a germplasm filter so it's not really a germplasm method. - Yvan 06/06/2024
    public ListWithPagination<ExperimentModel> getExpFromGermplasm(
            AccountModel currentUser,
            URI uri,
            String name,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return sparqlDAO.getExpFromGermplasm(currentUser, uri, name, orderByList, page, pageSize);

    }

    private GermplasmMetadataModel getStoredAttributes(URI uri) {
        GermplasmMetadataModel storedAttributes = null;
        try {
            storedAttributes = metaDataDao.get(uri);
        } catch (NoSQLInvalidURIException ignored) {
        }
        return storedAttributes;
    }

    public ListWithPagination<GermplasmModel> brapiSearch(AccountModel user, URI germplasmDbId, String germplasmName, String germplasmSpecies, int page, int pageSize) throws Exception {
        return sparqlDAO.brapiSearch(user, germplasmDbId, germplasmName, germplasmSpecies, page, pageSize);
    }

    /**
     * @return the list of existing URIs in germplasm graph among the given list of URIs
     */
    public Collection<URI> checkExistence(List<URI> uris) throws Exception {
        return sparqlDAO.sparql.getExistingUris(GermplasmModel.class, uris, true);
    }

    /**
     * Compute the list of unauthorized URIs among the provided list. Access to a germplasm is allowed if
     * one of the following conditions is fulfilled :
     *
     * <ul>
     *     <li>The user is admin</li>
     *     <li>The user is the publisher of the germplasm</li>
     *     <li>The germplasm is public</li>
     * </ul>
     */
    public Set<URI> getUnauthorizedGermplasms(Collection<URI> uris, AccountModel account) throws Exception {
        if (CollectionUtils.isEmpty(uris)) {
            return Set.of();
        }
        var filter = new GermplasmSearchFilter()
                .setUris(uris.stream().toList())
                .setUser(account);
        var allowedUris = search(filter, false, false).getList().stream()
                .map(GermplasmModel::getUri)
                .map(SPARQLDeserializers::formatURI)
                .collect(Collectors.toUnmodifiableSet());
        var initialUriSet = uris.stream().map(SPARQLDeserializers::formatURI).collect(Collectors.toSet());
        return SetUtils.difference(initialUriSet, allowedUris);
    }
}
