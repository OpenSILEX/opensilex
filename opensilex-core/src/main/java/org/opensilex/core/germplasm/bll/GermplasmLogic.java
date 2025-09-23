/*
 * *****************************************************************************
 *                         GermplasmLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 07/07/2025 13:21
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
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
import org.opensilex.server.exceptions.multipleError.MultipleErrorListException;
import org.opensilex.server.exceptions.multipleError.MultipleErrorObjectList;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

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
     * Use only for tests. Needed for mocking the dao. (dependency injection)
     */
    public GermplasmLogic(GermplasmDAO dao, SPARQLService sparql, AccountModel currentUser) {
        this.dao = dao;
        this.sparql = sparql;
        this.currentUser = currentUser;
    }

    /**
     * Create ( or update if uri already exists) a list of germplasm after checking the coherence of the data (see {@link #checkBeforeCreateOrUpdate(List, boolean)})
     * @param germplasmModels to create and/or update. A mix of both is possible.
     * @return updated or created germplasm as {@link List<GermplasmModel>}
     */
    public List<GermplasmModel> upsert(List<GermplasmModel> germplasmModels) throws Exception {
        Collection<URI> existingUris = checkExistence(germplasmModels.stream()
                .map(SPARQLResourceModel::getUri)
                .toList());

        List<GermplasmModel> germplasmModelsToUpdate = new ArrayList<>();
        List<GermplasmModel> germplasmModelsToCreate = new ArrayList<>();
        germplasmModels.forEach(germplasmModel -> {
            if ( germplasmModel.getUri() != null && SPARQLDeserializers.containsURI(existingUris, germplasmModel.getUri())) {
                germplasmModelsToUpdate.add(germplasmModel);
            } else {
                germplasmModelsToCreate.add(germplasmModel);
            }
        });

        var multipleErrorObject = checkBeforeCreateOrUpdate(germplasmModels, true);
        if (multipleErrorObject.hasErrors()){
            setIsUpdateForRelevantModels(germplasmModels, existingUris, multipleErrorObject);
            throw new MultipleErrorListException("getting errors while upserting germplasms", multipleErrorObject);
        }

        germplasmModels.forEach(this::retrieveLinkedSpeciesAndVariety);
        germplasmModels.forEach(germplasmModel -> germplasmModel.setPublisher(currentUser.getUri()));

        List<GermplasmModel> result = dao.createListWithoutUriExistsCheck(germplasmModelsToCreate);
        result.addAll(dao.updateList(germplasmModelsToUpdate));
        return result;
    }

    /**
     * Create a new germplasm after checking the coherence of the data (see {@link #checkBeforeCreateOrUpdate(List, boolean)})
     *
     * @param germplasmModel Germplasm to create
     * @return Created germplasm as {@link GermplasmModel}
     */
    public GermplasmModel create(GermplasmModel germplasmModel) throws Exception {
        var multipleErrorObjectList = checkBeforeCreateOrUpdate(Collections.singletonList(germplasmModel), true);
        if (multipleErrorObjectList.hasErrors()){
            throw new MultipleErrorListException("getting errors while creating germplasm", multipleErrorObjectList);
        }
        retrieveLinkedSpeciesAndVariety(germplasmModel);
        GermplasmModel model = germplasmModel;
        model.setPublisher(currentUser.getUri());
        model = dao.create(model);
        return model;
    }

    public GermplasmModel update(GermplasmModel germplasmModel) throws Exception {
        var multipleErrorObjectList = checkBeforeCreateOrUpdate(Collections.singletonList(germplasmModel), true);
        if (multipleErrorObjectList.hasErrors()){
            throw new MultipleErrorListException("getting errors while updating germplasm", multipleErrorObjectList);
        }
        retrieveLinkedSpeciesAndVariety(germplasmModel);
        return dao.update(germplasmModel,currentUser);
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
            dao.delete(instanceURI,currentUser);
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
     * Recherche paginée de {@link GermplasmModel} via le DAO selon les critères fournis.
     *
     * @param searchFilter       critères de recherche (filtres, pagination, tri, droits d’accès)
     * @param fetchMetadata      {@code true} pour charger aussi les métadonnées associées
     * @param fetchNestedObjects {@code true} pour charger aussi les objets liés (parents, relations, etc.)
     * @return liste paginée de {@link GermplasmModel} correspondant aux critères
     * @throws Exception si une erreur survient lors de la recherche
     */

    public ListWithPagination<GermplasmModel> search(
            GermplasmSearchFilter searchFilter,
            boolean fetchMetadata,
            boolean fetchNestedObjects
    ) throws Exception {
        return dao.search(searchFilter, fetchMetadata, fetchNestedObjects);
    }

    /**
     * See germplasm_import.md for more information about buisiness rules applied before creating or updating germplasms.
     * @return A map of errors with the key being the germplasm URI (as a string) and the value being the error message
     */
    public MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> checkBeforeCreateOrUpdate(
            List<GermplasmModel> germplasmModels,
            boolean update
    ) throws Exception {
        MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors = new MultipleErrorObjectList<>("germplasms errors", germplasmModels, MultipleCreateUpdateErrorObject::new);

        if (!update) {
            lookForAlreadyExistantUri(germplasmModels, errors);
        }

        globalFormatValidation(germplasmModels, errors);

        validateTypes(germplasmModels, errors);

        validateGermplasmDependenciesExists(germplasmModels, errors);

        validateAccessionVarietyOrSpeciesAreGiven(germplasmModels, errors);

        checkSpeciesCoherency(germplasmModels, errors);

        return errors;
    }

    /**
     * check the uri format of the germplasm, of its type and of the website. Check also the germplasm has a name
     */
    private void globalFormatValidation(
                    List<GermplasmModel> germplasmModels,
                    MultipleErrorObjectList<MultipleCreateUpdateErrorObject,
                    GermplasmModel> errors) {

        Map<URI, Boolean> uriValidationMap = new HashMap<>();

        germplasmModels.forEach(germplasmModel -> {
            if (germplasmModel.getUri() != null && !URIDeserializer.validateURI(germplasmModel.getUri().toString())) {
                errors.addError(germplasmModel, "Invalid URI format for URI: " + germplasmModel.getUri().toString());
            }
            if (!validateUriUsingMap(germplasmModel.getType(), uriValidationMap) ) {
                errors.addError(germplasmModel, getInvalidUriFormatOrNullErrorMessage(germplasmModel.getType(), "Germplasm type"));
            }
            if (germplasmModel.getLabel() == null || germplasmModel.getName().isBlank()) {
                errors.addError(germplasmModel, "Germplasm name is mandatory");
            }
            if (germplasmModel.getWebsite() != null && !URIDeserializer.validateURI(germplasmModel.getWebsite().toString())) {
                errors.addError(germplasmModel, "Invalid URI format for website: " + germplasmModel.getWebsite().toString());
            }
        });
    }

    /**
     * @return false if the URI is null or not valid, true otherwise.
     */
    private boolean validateUriUsingMap(URI uri, Map<URI, Boolean> uriValidationMap) {
        if (uri == null) {
            return false;
        }
        Boolean isValid = uriValidationMap.get(uri);
        if (isValid == null) {
            isValid = URIDeserializer.validateURI(uri.toString());
            uriValidationMap.put(uri, isValid);
        }
        return isValid;
    }

    /**
     * @param germplasmModels to check if they are not already in the database
     * @param errors map in which to put the errors
     */
    private void lookForAlreadyExistantUri(
                    List<GermplasmModel> germplasmModels,
                    MultipleErrorObjectList<MultipleCreateUpdateErrorObject,
                    GermplasmModel> errors) throws Exception {

        Collection<URI> existingUris = checkExistence(germplasmModels.stream()
                .map(SPARQLResourceModel::getUri)
                .toList());

        if (existingUris.isEmpty()) {
            return;
        }

        germplasmModels.forEach(germplasmModel -> {
            if (SPARQLDeserializers.containsURI(existingUris, germplasmModel.getUri())) {
                errors.addError(germplasmModel, "Germplasm URI already exists, it cannot be created again.");
            }
        });
    }

    /**
     * @param germplasmModels to check if their types exist in the database (basically if they are species, variety or accession)
     * @param errors map in which to put the errors. Error format : key = germplasm URI, value = error message (explaining which type doesn't exist)
     */
    private void validateTypes(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors) {
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
            Map<URI, List<GermplasmModel>> germplasmsErrorByType = new HashMap<>();
            nonExistingTypes.forEach(type -> germplasmsErrorByType.put(type, new ArrayList<>()));
            germplasmModels.forEach(germplasmModel -> {
                if (nonExistingTypes.contains(germplasmModel.getType())) {
                    germplasmsErrorByType.get(germplasmModel.getType()).add(germplasmModel);
                }
            });

            germplasmsErrorByType.forEach((type, uris) -> uris.forEach(germplasm -> errors.addError(germplasm, type+" : rdfType doesn't exist in the ontology")));
        }
    }

    /**
     * validate that every accession, variety or species, that one (or many) germplasm depend on, exist in the database and has the right type, and the URI is valid.
     */
    private void validateGermplasmDependenciesExists(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors) throws SPARQLException, DisplayableResponseException {

        //list every URI by type, type being species, variety or accession only if uri is valid
        Map<Resource, Set<URI>> urisByType = Map.of(Oeso.Species, new HashSet<>(), Oeso.Variety, new HashSet<>(), Oeso.Accession, new HashSet<>());
        Map<Resource, Map<URI, Boolean>> uriValidationMap = Map.of(Oeso.Species, new HashMap<>(), Oeso.Variety, new HashMap<>(), Oeso.Accession, new HashMap<>());

        germplasmModels.forEach(germplasmModel -> {
            if (germplasmModel.getSpecies() != null) {
                if (validateUriUsingMap(germplasmModel.getSpecies().getUri(), uriValidationMap.get(Oeso.Species))) {
                    urisByType.get(Oeso.Species).add(germplasmModel.getSpecies().getUri());
                }
                else errors.addError(germplasmModel,
                        getInvalidUriFormatOrNullErrorMessage(germplasmModel.getSpecies().getUri(), "Species"));
            }
            if (germplasmModel.getVariety() != null) {
                if (validateUriUsingMap(germplasmModel.getVariety().getUri(), uriValidationMap.get(Oeso.Variety))) {
                    urisByType.get(Oeso.Variety).add(germplasmModel.getVariety().getUri());
                }
                else errors.addError(germplasmModel,
                        getInvalidUriFormatOrNullErrorMessage(germplasmModel.getVariety().getUri(), "Variety"));
            }
            if (germplasmModel.getAccession() != null) {
                if (validateUriUsingMap(germplasmModel.getAccession().getUri(), uriValidationMap.get(Oeso.Accession))) {
                    urisByType.get(Oeso.Accession).add(germplasmModel.getAccession().getUri());
                }
                else errors.addError(germplasmModel,
                        getInvalidUriFormatOrNullErrorMessage(germplasmModel.getAccession().getUri(), "Accession"));
            }
        });

        //list every URI that doesn't exist in the database with the right type
        Map<Resource, List<URI>> uriThatDoesntExistWithThisType = new HashMap<>();
        for (Map.Entry<Resource, Set<URI>> entry : urisByType.entrySet()) {
            checkUrisExistsWithType(entry.getKey(), entry.getValue(), uriThatDoesntExistWithThisType);
        }

        // fill the errors map
        if (!uriThatDoesntExistWithThisType.isEmpty()) {
            germplasmModels.forEach(germplasmModel -> {
                if (germplasmModel.getSpecies() != null) {
                    var specieUri = germplasmModel.getSpecies().getUri();
                    var typeToCheck = Oeso.Species;
                    var nonExistingURI = uriThatDoesntExistWithThisType.get(typeToCheck);
                    addErrorIfUriDoesntExistForDependency(germplasmModel, typeToCheck, specieUri, nonExistingURI, errors);
                }
                if (germplasmModel.getVariety() != null) {
                    var varietyUri = germplasmModel.getVariety().getUri();
                    var typeToCheck = Oeso.Variety;
                    var nonExistingURI = uriThatDoesntExistWithThisType.get(typeToCheck);
                    addErrorIfUriDoesntExistForDependency(germplasmModel, typeToCheck, varietyUri, nonExistingURI, errors);
                }
                if (germplasmModel.getAccession() != null) {
                    var accessionUri = germplasmModel.getAccession().getUri();
                    var typeToCheck = Oeso.Accession;
                    var nonExistingURI = uriThatDoesntExistWithThisType.get(typeToCheck);
                    addErrorIfUriDoesntExistForDependency(germplasmModel, typeToCheck, accessionUri, nonExistingURI, errors);
                }
            });
        }
    }

    private String getInvalidUriFormatOrNullErrorMessage(URI uri, String typeName) {
        if (uri == null) {
            return typeName + " URI cannot be null.";
        }
        return "Invalid " + typeName + " URI format: " + uri;
    }

    private void addErrorIfUriDoesntExistForDependency(GermplasmModel germplasm, Resource type, URI dependencyUri, List<URI>notExistingURI, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors) {
        if (notExistingURI != null && SPARQLDeserializers.containsURI(notExistingURI, dependencyUri)) {
            errors.addError(germplasm, String.format("no %s found with this uri : %s .", type.getLocalName(), dependencyUri));
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
                    uriList = new ArrayList<>(List.of(uri));
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
    private void validateAccessionVarietyOrSpeciesAreGiven(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors) throws DisplayableResponseException {
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
                    errors.addError(germplasmModel, String.format(missingLinkMessage, typeOfGermplasmMissing));
                }
            }
        }
    }

    private void checkSpeciesCoherency(List<GermplasmModel> germplasmModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors){
        germplasmModels.forEach( germplasmModel -> {
            boolean isRelated;
            if (germplasmModel.getSpecies() != null && germplasmModel.getVariety() != null) {
                //check coherence between variety and species
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkVarietySpecies);
                if (!isRelated) {
                    String species = germplasmModel.getSpecies().getUri() != null ? germplasmModel.getSpecies().getUri().toString() : "null";
                    errors.addError(germplasmModel, "The given species doesn't match with the given variety. Wrong species : "+species);
                }
            }

            if (germplasmModel.getSpecies() != null && germplasmModel.getAccession() != null) {
                //check coherence between accession and species
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkAccessionSpecies);
                if (!isRelated) {
                    String species = germplasmModel.getSpecies().getUri() != null ? germplasmModel.getSpecies().getUri().toString() : "null";
                    errors.addError(germplasmModel, "The given species doesn't match with the given accession. Wrong species : "+species);
                }
            }

            if (germplasmModel.getVariety() != null && germplasmModel.getAccession() != null) {
                //check coherence between variety and accession
                isRelated = cache.get(new GermplasmLogic.Key(germplasmModel), this::checkAccessionVariety);
                if (!isRelated) {
                    String variety = germplasmModel.getVariety().getUri() != null ? germplasmModel.getVariety().getUri().toString() : "null";
                    errors.addError(germplasmModel, "The given variety doesn't match with the given accession. Wrong variety : "+variety);
                }
            }
        });
    }

    /**
     * Set the isUpdate property to true for the models that already exist in the database.
     * @param germplasmModels the list of germplasm models among which some may already exist in the database.
     * @param existingUris the URIs of the germplasm that already exist in the database.
     * @param multipleErrorObject the list of errors that will be updated with the isUpdate property for the models that already exist.
     */
    private static void setIsUpdateForRelevantModels(List<GermplasmModel> germplasmModels, Collection<URI> existingUris, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> multipleErrorObject) {
        Collection<GermplasmModel> modelsWithExistingUris = germplasmModels.stream()
                .filter(germplasmModel
                        -> existingUris.stream().anyMatch(existingUri
                            -> germplasmModel.getUri() != null && SPARQLDeserializers.compareURIs(germplasmModel.getUri(), existingUri)))
                .toList();
        multipleErrorObject.getModelsWithErrorsAsObjects()
                .forEach((modelAsObject, error) -> {
                    if (modelsWithExistingUris.stream().anyMatch( modelThatAlreadyExist ->  modelThatAlreadyExist == modelAsObject)) {
                        error.SetIsUpdate(true);
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

    /**
     * @return the uris of already existing germplasms. If Uris are not valid, they are ignored.
     */
    public Collection<URI> checkExistence(List<URI> uris) throws Exception {
        //sort uris to keep only well formatted ones
        List<URI> validUris = uris.stream()
                .filter(uri -> uri != null && !uri.toString().isBlank() && URIDeserializer.validateURI(uri.toString()))
                .toList();
        return dao.checkExistence(validUris);
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
