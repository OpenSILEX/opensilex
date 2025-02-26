/*
 * ******************************************************************************
 *  *                         StapleApiUtils.java
 *  * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 *  * Copyright © INRAE 2023.
 *  * Last Modification: 27/09/2023
 *  * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *  *
 *  *****************************************************************************
 */

package org.opensilex.core.germplasm.bll;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.exceptions.displayable.DisplayableResponseException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GermplasmLogic {

    private final GermplasmDAO dao;
    private final SPARQLService sparql;
    private final AccountModel currentUser;

    private static final Cache<GermplasmLogic.Key, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<GermplasmLogic.KeyType, Boolean> cacheType = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<GermplasmLogic.KeyGermplasm, GermplasmModel> cacheGermplasm = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public GermplasmLogic(SPARQLService sparql, MongoDBServiceV2 nosql, AccountModel currentUser) {
        this.dao = new GermplasmDAO(sparql, nosql);
        this.sparql = sparql;
        this.currentUser = currentUser;
    }

    /**
     * Create a new germplasm after checking the coherence of the data (see {@link #checkBeforeCreateOrUpdate(List, boolean)})
     *
     * @param germplasmModel Germplasm to create
     * @return Created germplasm as {@link GermplasmModel}
     */
    public GermplasmModel create(GermplasmModel germplasmModel) throws Exception, DisplayableResponseException {
        checkBeforeCreateOrUpdate(Collections.singletonList(germplasmModel), false);
        retrieveLinkedSpeciesAndVariety(germplasmModel);
        GermplasmModel model = germplasmModel;
        model.setPublisher(currentUser.getUri());
        model = dao.create(model);
        return model;
    }

    public List<GermplasmModel> create(List<GermplasmModel> germplasmModels) throws Exception, DisplayableResponseException {
        checkBeforeCreateOrUpdate(germplasmModels, false);
        germplasmModels.forEach(this::retrieveLinkedSpeciesAndVariety);
        germplasmModels.forEach(germplasmModel -> germplasmModel.setPublisher(currentUser.getUri()));
        return dao.createList(germplasmModels);
    }

    public GermplasmModel update(GermplasmModel germplasmModel) throws Exception {
        checkBeforeCreateOrUpdate(Collections.singletonList(germplasmModel), true);
        retrieveLinkedSpeciesAndVariety(germplasmModel);
        return dao.update(germplasmModel);
    }

    public void delete(URI instanceURI) throws Exception {
        GermplasmModel germplasmToDelete = dao.get(instanceURI, currentUser, false);

        if (germplasmToDelete == null) {
            throw new NotFoundURIException("Invalid or unknown Germplasm URI ", instanceURI);
        }

        if (dao.isLinkedToSth(germplasmToDelete)) {
            throw new BadRequestException(String.format(
                    "germplasm with URI '%s' can't be deleted. It is already linked to another germplasm or a scientific object or an experiment."
                    , instanceURI.toString()));
        } else {
            dao.delete(instanceURI);
        }
    }

    /**
     * Get a germplasm by its URI
     *
     * @param instanceURI URI of the germplasm
     * @param withNested  boolean to indicate if nested objects should be fetched (parent germplasms)
     * @return GermplasmModel
     */
    public GermplasmModel get(URI instanceURI, boolean withNested) throws Exception {
        return dao.get(instanceURI, currentUser, withNested);
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
        return dao.search(searchFilter, fetchMetadata, fetchNestedObjects);
    }

    public void checkBeforeCreateOrUpdate(List<GermplasmModel> germplasmModels, boolean update) throws
            Exception, DisplayableResponseException {

        if (!update) {
            validateUrisDoesNotExistOrThrow(germplasmModels.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList()));
        }

        validateTypesOrThrow(germplasmModels);

        //Check that the given fromAccession, fromVariety or fromSpecies exist in DB
        List<URI> speciesUris = new ArrayList<>();
        List<URI> varietyUris = new ArrayList<>();
        List<URI> accessionUris = new ArrayList<>();
        germplasmModels.forEach(germplasmModel -> {
            if (germplasmModel.getSpecies() != null) {
                speciesUris.add(germplasmModel.getSpecies().getUri());
            }
            if (germplasmModel.getVariety() != null) {
                varietyUris.add(germplasmModel.getVariety().getUri());
            }
            if (germplasmModel.getAccession() != null) {
                accessionUris.add(germplasmModel.getAccession().getUri());
            }
        });

        if ( ! speciesUris.isEmpty() ) {
            validateUrisExistOrThrow(
                    speciesUris,
                    new URI(Oeso.Species.getURI()),
                    "species",
                    "component.germplasms.errors.unknownSpecies",
                    "unknownSpecies");
        }

        if ( ! varietyUris.isEmpty() ) {
            validateUrisExistOrThrow(
                    varietyUris,
                    new URI(Oeso.Variety.getURI()),
                    "variety",
                    "component.germplasms.errors.unknownVariety",
                    "unknownVariety");
        }

        if ( ! accessionUris.isEmpty() ) {
            validateUrisExistOrThrow(
                    accessionUris,
                    new URI(Oeso.Accession.getURI()),
                    "accession",
                    "component.germplasms.errors.unknownAccession",
                    "unknownAccession");
        }

        validateAccessionVarietyOrSpeciesAreGivenOrThrow(germplasmModels);

        // check coherence between species, variety and accession
        germplasmModels.forEach( germplasmModel -> {
            boolean isRelated;
            if (germplasmModel.getSpecies() != null && germplasmModel.getVariety() != null) {
                //check coherence between variety and species
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkVarietySpecies);
                if (!isRelated) {
                    throw new DisplayableResponseException(
                            "wrong species : " + germplasmModel.getSpecies().toString(),
                            Response.Status.BAD_REQUEST,
                            "The given species doesn't match with the given variety",
                            null,
                            null
                    );
                }
            }

            if (germplasmModel.getSpecies() != null && germplasmModel.getAccession() != null) {
                //check coherence between accession and species
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkAccessionSpecies);
                if (!isRelated) {
                    throw new DisplayableResponseException(
                            "wrong species : " + germplasmModel.getSpecies().toString(),
                            Response.Status.BAD_REQUEST,
                            "The given species doesn't match with the given variety",
                            null,
                            null
                    );
                }
            }

            if (germplasmModel.getVariety() != null && germplasmModel.getAccession() != null) {
                //check coherence between variety and accession
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkAccessionVariety);
                if (!isRelated) {
                    throw new DisplayableResponseException(
                            "wrong species : " + germplasmModel.getSpecies().toString(),
                            Response.Status.BAD_REQUEST,
                            "The given species doesn't match with the given variety",
                            null,
                            null
                    );
                }
            }
        });
    }

    private void validateUrisDoesNotExistOrThrow(List<URI> germplasmsUris) throws SPARQLException, DisplayableResponseException {
        Set<URI> uniqueUris = new HashSet<>(germplasmsUris);
        Set<URI> nonExistingUris = new HashSet<>();
        for (URI germplasmUri : uniqueUris) {
            if (sparql.uriExists(GermplasmModel.class, germplasmUri)) {
                nonExistingUris.add(germplasmUri);
            }
        }

        if (!nonExistingUris.isEmpty()) {
            throw new DisplayableResponseException(
                    "Duplicated URIs: " + nonExistingUris,
                    Response.Status.CONFLICT,
                    "Germplasm URI already exists",
                    "component.germplasms.errors.duplicateUri",
                    new HashMap<>() {{
                        put("uri", nonExistingUris.toString());
                    }}
            );
        }
    }

    private void validateTypesOrThrow(List<GermplasmModel> germplasmModels) throws DisplayableResponseException{
        Set<URI> uniqueTypes = germplasmModels.stream()
                .map(GermplasmModel::getType)
                .collect(Collectors.toSet());
        Set<URI> nonExistingTypes = new HashSet<>();
        for (URI type : uniqueTypes) {
            boolean isType = cacheType.get(new GermplasmLogic.KeyType(type), this::checkType);
            if (!isType) {
                nonExistingTypes.add(type);
            }
        }

        if (!nonExistingTypes.isEmpty()) {
            throw new DisplayableResponseException(
                    "Unknown types: " + nonExistingTypes,
                    Response.Status.BAD_REQUEST,
                    "rdfType doesn't exist in the ontology",
                    "component.germplasms.errors.wrongRdfType",
                    new HashMap<>() {{
                        put("type", nonExistingTypes.toString());
                    }}
            );
        }
    }

    private void validateUrisExistOrThrow(List<URI> uris, URI rdfType, String type, String errorTranslationKey, String keyErrorTranslationValues) throws SPARQLException, DisplayableResponseException {
        Set<URI> uniqueUris = new HashSet<>(uris);
        Set<URI> nonExistingUris = new HashSet<>();
        for (URI uri : uniqueUris) {
            if (!sparql.uriExists(rdfType, uri)) {
                nonExistingUris.add(uri);
            }
        }

        if (!nonExistingUris.isEmpty()) {
            throw new DisplayableResponseException(
                    "Unknown "+type+": " + nonExistingUris,
                    Response.Status.BAD_REQUEST,
                    "The given "+type+" doesn't exist in the database",
                    errorTranslationKey,
                    new HashMap<>() {{
                        put(keyErrorTranslationValues, nonExistingUris.toString());
                    }}
            );
        }
    }

    private void validateAccessionVarietyOrSpeciesAreGivenOrThrow(List<GermplasmModel> germplasmModels) throws DisplayableResponseException {
        Map<Integer, String> messages = Map.of(
                0, "species",
                1, "variety or species",
                2, "accession, variety or species"
        );
        int missingLinkMessage = 0;
        List<URI> urisWithMissingLink = new ArrayList<>();

        for (GermplasmModel germplasmModel : germplasmModels) {
            if (SPARQLDeserializers.compareURIs(germplasmModel.getType().toString(), Oeso.Species.getURI())) {
                break;
            } else if (SPARQLDeserializers.compareURIs(germplasmModel.getType().toString(), Oeso.Variety.getURI())) {
                if (germplasmModel.getSpecies() == null) {
                    urisWithMissingLink.add(germplasmModel.getUri());
                }
            } else if (SPARQLDeserializers.compareURIs(germplasmModel.getType().toString(), Oeso.Accession.getURI())) {
                if (germplasmModel.getSpecies() == null && germplasmModel.getVariety() == null) {
                    urisWithMissingLink.add(germplasmModel.getUri());
                }
                missingLinkMessage = Math.max(missingLinkMessage, 1);
            } else {
                if (germplasmModel.getSpecies() == null && germplasmModel.getVariety() == null && germplasmModel.getAccession() == null) {
                    urisWithMissingLink.add(germplasmModel.getUri());
                }
                missingLinkMessage = 2;
            }
        }

        if (!urisWithMissingLink.isEmpty()) {
            final String finalMessage = messages.get(missingLinkMessage);
            throw new DisplayableResponseException(
                    "you have to fill " + finalMessage + " for the following germplasms : " + urisWithMissingLink,
                    Response.Status.BAD_REQUEST,
                    "missing attribute",
                    "component.germplasms.errors.missingAttribute",
                    new HashMap<>() {{
                        put("message", finalMessage);
                    }}
            );
        }
    }

    /**
     * Get all experiments associated with a germplasm
     */
    public ListWithPagination<ExperimentModel> getExpFromGermplasm(
            AccountModel currentUser,
            URI uri,
            String name,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return dao.getExpFromGermplasm(currentUser, uri, name, orderByList, page, pageSize);

    }

    /**
     * @return all germplasm attributes (key). Each attribute is unique
     * */
    public Set<String> getDistinctGermplasmAttributes() {
        return dao.getDistinctGermplasmAttributes();
    }

    /**
     * @param attribute the attribute key
     * @param attributeValue the attribute search pattern
     * @return each unique attribute values for the given attribute from the germplasm collection
     * */
    public Set<String> getDistinctGermplasmAttributesValues(String attribute, String attributeValue, int page, int pageSize) {
        return dao.getDistinctGermplasmAttributesValues(attribute, attributeValue, page, pageSize);
    }

    //#region private methods

    /**
     * Sets a species if a variety was given. Sets species and variety if an accession was given.
     */
    private void retrieveLinkedSpeciesAndVariety(GermplasmModel germplasmModel) {
        if (germplasmModel.getSpecies() == null && germplasmModel.getVariety() != null) {
            GermplasmModel variety = cacheGermplasm.get(new GermplasmLogic.KeyGermplasm(germplasmModel.getVariety().getUri()), this::getGermplasm);
            if (variety != null) {
                germplasmModel.setSpecies(variety.getSpecies());
            }

        }
        if (germplasmModel.getAccession() != null && germplasmModel.getVariety() == null && germplasmModel.getSpecies() == null) {
            GermplasmModel accession = cacheGermplasm.get(new GermplasmLogic.KeyGermplasm(germplasmModel.getAccession().getUri()), this::getGermplasm);
            if (accession != null) {
                germplasmModel.setVariety(accession.getVariety());
                germplasmModel.setSpecies(accession.getSpecies());
            }
        }
    }

    private GermplasmModel getGermplasm(GermplasmLogic.KeyGermplasm key) {
        try {
            return dao.get(key.uri, currentUser, false);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean checkType(GermplasmLogic.KeyType key) {
        try {
            return dao.isGermplasmType(key.type);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkVarietySpecies(GermplasmLogic.Key key) {
        return checkVarietySpecies(key.species, key.variety);
    }

    private boolean checkVarietySpecies(URI speciesURI, URI varietyURI) {
        try {
            GermplasmModel variety = dao.get(varietyURI, currentUser, false);
            return SPARQLDeserializers.compareURIs(variety.getSpecies().getUri().toString(), speciesURI.toString());
        } catch (Exception e) {
            return true; //the variety doesn't exist in the database yet
        }
    }

    private boolean checkAccessionSpecies(GermplasmLogic.Key key) {
        return checkAccessionSpecies(key.species, key.variety);
    }

    private boolean checkAccessionSpecies(URI speciesURI, URI accessionURI) {
        try {
            GermplasmModel accession = dao.get(accessionURI, currentUser, false);
            return SPARQLDeserializers.compareURIs(accession.getSpecies().getUri().toString(), speciesURI.toString());
        } catch (Exception e) {
            return true; //the accession doesn't exist in the database yet
        }
    }

    private boolean checkAccessionVariety(GermplasmLogic.Key key) {
        return checkAccessionVariety(key.species, key.variety);
    }

    private boolean checkAccessionVariety(URI varietyURI, URI accessionURI) {
        try {
            GermplasmModel accession = dao.get(accessionURI, currentUser, false);
            return SPARQLDeserializers.compareURIs(accession.getVariety().getUri().toString(), varietyURI.toString());
        } catch (Exception e) {
            return true; //the accession doesn't exist in the database yet
        }
    }
    //#endregion

    //#region internal classes
    private static final class Key {

        final URI species;
        final URI variety;

        private Key(GermplasmModel germplasmModel) {
            species = germplasmModel.getSpecies().getUri();
            variety = germplasmModel.getVariety().getUri();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(this.species));
            hash = 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(this.variety));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GermplasmLogic.Key other = (GermplasmLogic.Key) obj;
            if (!SPARQLDeserializers.compareURIs(this.species.toString(), other.species.toString())) {
                return false;
            }
            return SPARQLDeserializers.compareURIs(this.variety.toString(), other.variety.toString());
        }

    }

    private static final class KeyType {

        final URI type;

        public KeyType(URI type) {
            this.type = type;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GermplasmLogic.KeyType other = (GermplasmLogic.KeyType) obj;
            return SPARQLDeserializers.compareURIs(this.type.toString(), other.type.toString());
        }
    }

    private static class KeyGermplasm {

        final URI uri;

        public KeyGermplasm(URI uri) {
            this.uri = uri;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.uri);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GermplasmLogic.KeyGermplasm other = (GermplasmLogic.KeyGermplasm) obj;
            return SPARQLDeserializers.compareURIs(this.uri.toString(), other.uri.toString());
        }
    }
    //#endregion
}
