//******************************************************************************
//                          GermplasmDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.bson.Document;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
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
 * Data Access Object for Germplasm, metadatas (also known as attributes) are stored in MongoDB while the rest is stored in RDF.
 * @author Alice Boizet
 */
public class GermplasmDAO {

    protected final SPARQLService sparql;
    protected final MongoDBServiceV2 nosql;
    private final GermplasmSparqlDAO sparqlDAO;
    private final MetaDataDaoV2 metaDataDao;
    public static final String ATTRIBUTES_COLLECTION_NAME = "germplasmAttribute";

    public GermplasmDAO(SPARQLService sparql, MongoDBServiceV2 nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.sparqlDAO = new GermplasmSparqlDAO(sparql);
        this.metaDataDao = new MetaDataDaoV2(nosql, ATTRIBUTES_COLLECTION_NAME);

        MongoCollection<MetaDataModel> collection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, MetaDataModel.class);
        collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
    }

    public GermplasmDAO(SPARQLService sparql, MongoDBService nosql) {
        this(sparql, nosql.getServiceV2());
    }

    public GermplasmModel update(GermplasmModel model) throws Exception {
        MetaDataModel storedAttributes = getStoredAttributes(model.getUri());
        MetaDataModel attributeModel = model.getMetadata();

        if (((attributeModel == null || MapUtils.isEmpty(attributeModel.getAttributes())) && storedAttributes == null)) {
            sparqlDAO.update(model);
        } else {
            new SparqlMongoTransaction(sparql,nosql).execute(session -> {
                sparqlDAO.update(model);

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

    public GermplasmModel create(GermplasmModel model) throws Exception {
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
     * Get a germplasm by its URI
     * @param uri URI of the germplasm
     * @param user User account
     * @param withNested boolean to indicate if nested objects should be fetched (parent germplasms)
     * @return GermplasmModel
     */
    public GermplasmModel get(URI uri, AccountModel user, boolean withNested) throws Exception {
        GermplasmModel germplasm = sparqlDAO.get(uri, user, withNested);
        if (germplasm != null) {
            MetaDataModel storedAttributes = getStoredAttributes(germplasm.getUri());
            if (storedAttributes != null) {
                germplasm.setMetadata(storedAttributes);
            }
        }
        return germplasm;
    }

    /**
     * @param searchFilter  search filter
     * @param fetchMetadata indicate if {@link GermplasmModel#getMetadata()} must be retrieved from mongodb
     * @param fetchNestedObjects if true, fetch nested objects (parent germplasms)
     * @return a {@link ListWithPagination} of {@link GermplasmModel}
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

        ListWithPagination<GermplasmModel> models = sparqlDAO.search(searchFilter, fetchNestedObjects);

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

    public void delete(URI uri) throws Exception {
        
        new SparqlMongoTransaction(sparql,nosql).execute(session -> {
            if (metaDataDao.exists(uri)) {
                metaDataDao.delete(session, uri);
            }
            sparqlDAO.delete(uri);
            return null;
        });
    }

    public boolean isLinkedToSth(GermplasmModel germplasm) throws SPARQLException {
        return sparqlDAO.isLinkedToSth(germplasm);
    }

    /**
     * Get all germplasm attributes
     */
    public Set<String> getDistinctGermplasmAttributes() {
        return metaDataDao.getDistinctKeys();
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

    private MetaDataModel getStoredAttributes(URI uri) {
        MetaDataModel storedAttributes = null;
        try {
            storedAttributes = metaDataDao.get(uri);
        } catch (NoSQLInvalidURIException ignored) {
        }
        return storedAttributes;
    }

    public ListWithPagination<GermplasmModel> brapiSearch(AccountModel user, URI germplasmDbId, String germplasmName, String germplasmSpecies, int page, int pageSize) throws Exception {
        return sparqlDAO.brapiSearch(user, germplasmDbId, germplasmName, germplasmSpecies, page, pageSize);
    }


}
