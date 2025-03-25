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
import org.apache.jena.rdf.model.Resource;
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
import org.opensilex.server.exceptions.multipleError.MultipleCreateUpdateErrorObject;
import org.opensilex.server.exceptions.multipleError.MultipleErrorException;
import org.opensilex.server.exceptions.multipleError.MultipleErrorObjectList;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
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
    public GermplasmModel create(GermplasmModel germplasmModel) throws Exception {
        var multipleErrorObjectList = checkBeforeCreateOrUpdate(Collections.singletonList(germplasmModel), false);
        if (multipleErrorObjectList.hasErrors()){
            throw new MultipleErrorException("getting errors while creating germplasm", multipleErrorObjectList);
        }
        retrieveLinkedSpeciesAndVariety(germplasmModel);
        GermplasmModel model = germplasmModel;
        model.setPublisher(currentUser.getUri());
        model = dao.create(model);
        return model;
    }

    public List<GermplasmModel> create(List<GermplasmModel> germplasmModels) throws Exception {
        var multipleErrorObject = checkBeforeCreateOrUpdate(germplasmModels, false);
        if (multipleErrorObject.hasErrors()){
            throw new MultipleErrorException("getting errors while creating germplasms", multipleErrorObject);
        }

        germplasmModels.forEach(this::retrieveLinkedSpeciesAndVariety);
        germplasmModels.forEach(germplasmModel -> germplasmModel.setPublisher(currentUser.getUri()));
        return dao.createListWithoutUriExistsCheck(germplasmModels);
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

    /**
     * @return A map of errors with the key being the germplasm URI (as a string) and the value being the error message
     */
    public MultipleErrorObjectList<MultipleCreateUpdateErrorObject> checkBeforeCreateOrUpdate(List<GermplasmModel> germplasmModels, boolean update) throws SPARQLException {
        MultipleErrorObjectList<MultipleCreateUpdateErrorObject> errors = new MultipleErrorObjectList<>("germplasms errors", MultipleCreateUpdateErrorObject::new);

        if (!update) {
            var uriList = germplasmModels.stream().map(SPARQLResourceModel::getUri).toList();
            lookForAlreadyExistantUri(uriList, errors);
        }

        validateTypes(germplasmModels, errors);

        validateGermplasmDependenciesExists(germplasmModels, errors);

        validateAccessionVarietyOrSpeciesAreGiven(germplasmModels, errors);

        checkSpeciesCoherency(germplasmModels, errors);

        return errors;
    }

    /**
     * @param germplasmsUris to check if they are not already in the database
     * @param errors map in which to put the errors
     */
    private void lookForAlreadyExistantUri(List<URI> germplasmsUris, MultipleErrorObjectList<MultipleCreateUpdateErrorObject> errors) throws SPARQLException {
        Set<URI> uniqueUris = new HashSet<>(germplasmsUris);
        Set<URI> nonExistingUris = new HashSet<>();
        for (URI germplasmUri : uniqueUris) {
            if (sparql.uriExists(GermplasmModel.class, germplasmUri)) {
                nonExistingUris.add(germplasmUri);
            }
        }

        nonExistingUris.forEach(uri -> errors.addError(uri.toString(), "Germplasm URI already exists"));
    }

    /**
     * @param germplasmModels to check if their types exist in the database (basically if they are species, variety or accession)
     * @param errors map in which to put the errors. Error format : key = germplasm URI, value = error message (explaining which type doesn't exist)
     */
    private void validateTypes(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject> errors) {
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
            Map<URI, List<URI>> germplasmsErrorByType = new HashMap<>();
            nonExistingTypes.forEach(type -> germplasmsErrorByType.put(type, new ArrayList<>()));
            germplasmModels.forEach(germplasmModel -> {
                if (nonExistingTypes.contains(germplasmModel.getType())) {
                    germplasmsErrorByType.get(germplasmModel.getType()).add(germplasmModel.getUri());
                }
            });

            germplasmsErrorByType.forEach((type, uris) -> uris.forEach(uri -> errors.addError(uri.toString(), type+" : rdfType doesn't exist in the ontology")));
        }
    }

    /**
     * validate that every accession, variety or species, that one (or many) germplasm depend on, exist in the database and has the right type
     */
    private void validateGermplasmDependenciesExists(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject> errors) throws SPARQLException, DisplayableResponseException {
        //list every URI by type, type being species, variety or accession
        Map<Resource, Set<URI>> urisByType = Map.of(Oeso.Species, new HashSet<>(), Oeso.Variety, new HashSet<>(), Oeso.Accession, new HashSet<>());
        germplasmModels.forEach(germplasmModel -> {
            if (germplasmModel.getSpecies() != null) {
                urisByType.get(Oeso.Species).add(germplasmModel.getSpecies().getUri());
            }
            if (germplasmModel.getVariety() != null) {
                urisByType.get(Oeso.Variety).add(germplasmModel.getVariety().getUri());
            }
            if (germplasmModel.getAccession() != null) {
                urisByType.get(Oeso.Accession).add(germplasmModel.getAccession().getUri());
            }
        });

        //list every URI that doesn't exist in the database with the right type
        Map<Resource, List<URI>> uriThatDoesntExistWithThisType = new HashMap<>();
        for (Map.Entry<Resource, Set<URI>> entry : urisByType.entrySet()) {
            checkUrisExistsWithType(entry.getKey(), entry.getValue(), uriThatDoesntExistWithThisType);
        }

        BiConsumer<URI, Resource> addErrorIfUriDoesntExistWithType = (uri, type) -> {
            var uriList = uriThatDoesntExistWithThisType.get(type);
            if (uriList != null && uriList.contains(uri)) {
                errors.addError(uri.toString(), String.format("no %s found with this uri : %s .",type.getLocalName() ,uri));
            }
        };

        // fill the errors map
        if (!uriThatDoesntExistWithThisType.isEmpty()) {
            germplasmModels.forEach(germplasmModel -> {
                if (germplasmModel.getSpecies() != null) {
                    var specieUri = germplasmModel.getSpecies().getUri();
                    var typeToCheck = Oeso.Species;
                    addErrorIfUriDoesntExistWithType.accept(specieUri, typeToCheck);
                }
                if (germplasmModel.getVariety() != null) {
                    var varietyUri = germplasmModel.getVariety().getUri();
                    var typeToCheck = Oeso.Variety;
                    addErrorIfUriDoesntExistWithType.accept(varietyUri, typeToCheck);
                }
                if (germplasmModel.getAccession() != null) {
                    var accessionUri = germplasmModel.getAccession().getUri();
                    var typeToCheck = Oeso.Accession;
                    addErrorIfUriDoesntExistWithType.accept(accessionUri, typeToCheck);
                }
            });
        }
    }

    /**
     * if a URI does not exist with the good type, add it to the map uriThatDoesntExistWithThisType
     */
    private void checkUrisExistsWithType(Resource type, Collection<URI> uris, Map<Resource, List<URI>> uriThatDoesntExistWithThisType) throws SPARQLException {
        URI typeURI = URI.create(type.getURI());

        for (URI uri : uris) {
            if (!sparql.uriExists(typeURI, uri)) {
                var uriList = uriThatDoesntExistWithThisType.get(type);
                if (uriList == null) {
                    uriList = new ArrayList<>();
                    uriThatDoesntExistWithThisType.put(type, uriList);
                } else {
                    uriList.add(uri);
                }
            }
        }
    }

    /**
     * A Variety should have a species, an Accession should have a variety or a species, and other types should have a specie, a variety or an accession
     */
    private void validateAccessionVarietyOrSpeciesAreGiven(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject> errors) throws DisplayableResponseException {
        Map<String, String> messages = Map.of(
                SPARQLDeserializers.getExpandedURI(Oeso.Variety.getURI()), "species",
                SPARQLDeserializers.getExpandedURI(Oeso.Accession.getURI()), "variety or species"
        );
        String missingLinkMessage = "you have to fill %s for this germplam";

        for (GermplasmModel germplasmModel : germplasmModels) {

            if (SPARQLDeserializers.compareURIs(germplasmModel.getType().toString(), Oeso.Species.getURI())) { break; }
            else {
                if (germplasmModel.getSpecies() == null && germplasmModel.getVariety() == null && germplasmModel.getAccession() == null) {
                    String typeOfGermplasmMissing = messages.getOrDefault(SPARQLDeserializers.getExpandedURI(germplasmModel.getType().toString()),
                            "accession, variety or species");
                    errors.addError(germplasmModel.getUri().toString(), String.format(missingLinkMessage, typeOfGermplasmMissing));
                }
            }
        }
    }

    private void checkSpeciesCoherency(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject> errors){
        germplasmModels.forEach( germplasmModel -> {
            boolean isRelated;
            if (germplasmModel.getSpecies() != null && germplasmModel.getVariety() != null) {
                //check coherence between variety and species
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkVarietySpecies);
                if (!isRelated) {
                    errors.addError(germplasmModel.getUri().toString(), "The given species doesn't match with the given variety. Wrong species : "+germplasmModel.getSpecies().toString());
                }
            }

            if (germplasmModel.getSpecies() != null && germplasmModel.getAccession() != null) {
                //check coherence between accession and species
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkAccessionSpecies);
                if (!isRelated) {
                    errors.addError(germplasmModel.getUri().toString(), "The given species doesn't match with the given accession. Wrong species : "+germplasmModel.getSpecies().toString());
                }
            }

            if (germplasmModel.getVariety() != null && germplasmModel.getAccession() != null) {
                //check coherence between variety and accession
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkAccessionVariety);
                if (!isRelated) {
                    errors.addError(germplasmModel.getUri().toString(), "The given variety doesn't match with the given accession. Wrong variety : "+germplasmModel.getVariety().toString());
                }
            }
        });
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
