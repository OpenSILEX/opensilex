/*
 * *****************************************************************************
 *                         GeneticResourceLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 07/07/2025 13:21
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.geneticResource.bll;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geneticResource.api.GeneticResourceSearchFilter;
import org.opensilex.core.geneticResource.dal.GeneticResourceDAO;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundException;
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

public class GeneticResourceLogic {

    private final GeneticResourceDAO dao;
    private final SPARQLService sparql;
    private final AccountModel currentUser;

    private static final Cache<GeneticResourceCoherencyCacheKey, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<GeneticResourceLogic.KeyType, Boolean> cacheType = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<GeneticResourceLogic.KeyGeneticResource, GeneticResourceModel> cacheGeneticResource = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public GeneticResourceLogic(SPARQLService sparql, MongoDBServiceV2 nosql, AccountModel currentUser) {
        this.dao = new GeneticResourceDAO(sparql, nosql);
        this.sparql = sparql;
        this.currentUser = currentUser;
    }

    /**
     * Use only for tests. Needed for mocking the dao. (dependency injection)
     */
    public GeneticResourceLogic(GeneticResourceDAO dao, SPARQLService sparql, AccountModel currentUser) {
        this.dao = dao;
        this.sparql = sparql;
        this.currentUser = currentUser;
    }

    /**
     * Create ( or update if uri already exists) a list of geneticResource after checking the coherence of the data (see {@link #checkBeforeCreateOrUpdate(List, boolean)})
     * @param geneticResourceModels to create and/or update. A mix of both is possible.
     * @return updated or created geneticResource as {@link List<GeneticResourceModel>}
     */
    public List<GeneticResourceModel> upsert(List<GeneticResourceModel> geneticResourceModels) throws Exception {
        Collection<URI> existingUris = getNonExistingUris(geneticResourceModels.stream()
                .map(SPARQLResourceModel::getUri)
                .toList());

        List<GeneticResourceModel> geneticResourceModelsToUpdate = new ArrayList<>();
        List<GeneticResourceModel> geneticResourceModelsToCreate = new ArrayList<>();
        geneticResourceModels.forEach(geneticResourceModel -> {
            if ( geneticResourceModel.getUri() != null && SPARQLDeserializers.containsURI(existingUris, geneticResourceModel.getUri())) {
                geneticResourceModelsToUpdate.add(geneticResourceModel);
            } else {
                geneticResourceModelsToCreate.add(geneticResourceModel);
            }
        });

        var multipleErrorObject = checkBeforeCreateOrUpdate(geneticResourceModels, true);
        if (multipleErrorObject.hasErrors()){
            setIsUpdateForRelevantModels(geneticResourceModels, existingUris, multipleErrorObject);
            throw new MultipleErrorListException("getting errors while upserting geneticResources", multipleErrorObject);
        }

        geneticResourceModels.forEach(this::retrieveLinkedSpeciesAndVariety);
        geneticResourceModels.forEach(geneticResourceModel -> geneticResourceModel.setPublisher(currentUser.getUri()));

        List<GeneticResourceModel> result = dao.createListWithoutUriExistsCheck(geneticResourceModelsToCreate);
        result.addAll(dao.updateList(geneticResourceModelsToUpdate));
        return result;
    }

    /**
     * Create a new geneticResource after checking the coherence of the data (see {@link #checkBeforeCreateOrUpdate(List, boolean)})
     *
     * @param geneticResourceModel GeneticResource to create
     * @return Created geneticResource as {@link GeneticResourceModel}
     */
    public GeneticResourceModel create(GeneticResourceModel geneticResourceModel) throws Exception {
        var multipleErrorObjectList = checkBeforeCreateOrUpdate(Collections.singletonList(geneticResourceModel), false);
        if (multipleErrorObjectList.hasErrors()){
            throw new MultipleErrorListException("getting errors while creating geneticResource", multipleErrorObjectList);
        }
        retrieveLinkedSpeciesAndVariety(geneticResourceModel);
        GeneticResourceModel model = geneticResourceModel;
        model.setPublisher(currentUser.getUri());
        model = dao.create(model);
        return model;
    }

    public GeneticResourceModel update(GeneticResourceModel geneticResourceModel) throws Exception {
        if (geneticResourceModel.getUri() == null){
            throw new BadRequestException("GeneticResource URI cannot be null for update.");
        }
        if (getNonExistingUris(List.of(geneticResourceModel.getUri())).isEmpty()){
            throw new NotFoundException(String.format("GeneticResource URI %s not found.", geneticResourceModel.getUri()));
        }
        var multipleErrorObjectList = checkBeforeCreateOrUpdate(Collections.singletonList(geneticResourceModel), true);
        if (multipleErrorObjectList.hasErrors()){
            throw new MultipleErrorListException("getting errors while updating geneticResource", multipleErrorObjectList);
        }
        retrieveLinkedSpeciesAndVariety(geneticResourceModel);
        return dao.update(geneticResourceModel,currentUser);
    }

    public void delete(URI instanceURI) throws Exception {
        GeneticResourceModel geneticResourceToDelete = dao.get(instanceURI, currentUser, false);

        if (geneticResourceToDelete == null) {
            throw new NotFoundURIException("Invalid or unknown GeneticResource URI ", instanceURI);
        }

        if (dao.isLinkedToSth(geneticResourceToDelete)) {
            throw new BadRequestException(String.format(
                    "geneticResource with URI '%s' can't be deleted. It is already linked to another geneticResource or a scientific object or an experiment."
                    , instanceURI.toString()));
        } else {
            dao.delete(instanceURI,currentUser);
        }
    }

    /**
     * Get a geneticResource by its URI
     *
     * @param instanceURI URI of the geneticResource
     * @param withNested  boolean to indicate if nested objects should be fetched (parent geneticResources)
     * @return GeneticResourceModel
     */
    public GeneticResourceModel get(URI instanceURI, boolean withNested) throws Exception {
        return dao.get(instanceURI, currentUser, withNested);
    }

    /**
     * Paginated search of {@link GeneticResourceModel} via the DAO according to the provided criteria.
     *
     * @param searchFilter        search criteria (filters, pagination, sorting, access rights)
     * @param fetchMetadata       {@code true} to also load associated metadata
     * @param fetchNestedObjects  {@code true} to also load related objects (parents, relationships, etc.)
     * @return a paginated list of {@link GeneticResourceModel} matching the criteria
     * @throws Exception if an error occurs during the search
     */

    public ListWithPagination<GeneticResourceModel> search(
            GeneticResourceSearchFilter searchFilter,
            boolean fetchMetadata,
            boolean fetchNestedObjects
    ) throws Exception {
        return dao.search(searchFilter, fetchMetadata, fetchNestedObjects);
    }

    /**
     * See geneticResource_import.md for more information about buisiness rules applied before creating or updating geneticResources.
     * @return A map of errors with the key being the geneticResource URI (as a string) and the value being the error message
     */
    public MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> checkBeforeCreateOrUpdate(
            List<GeneticResourceModel> geneticResourceModels,
            boolean update
    ) throws Exception {
        MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors = new MultipleErrorObjectList<>("geneticResources errors", geneticResourceModels, MultipleCreateUpdateErrorObject::new);

        if (!update) {
            lookForAlreadyExistantUri(geneticResourceModels, errors);
        }

        globalFormatValidation(geneticResourceModels, errors);

        validateTypes(geneticResourceModels, errors);

        validateGeneticResourceDependenciesExists(geneticResourceModels, errors);

        validateAccessionVarietyOrSpeciesAreGiven(geneticResourceModels, errors);

        checkSpeciesCoherency(geneticResourceModels, errors);

        return errors;
    }

    /**
     * check the uri format of the geneticResource, of its type and of the website. Check also the geneticResource has a name
     */
    private void globalFormatValidation(
                    List<GeneticResourceModel> geneticResourceModels,
                    MultipleErrorObjectList<MultipleCreateUpdateErrorObject,
                    GeneticResourceModel> errors) {

        Map<URI, Boolean> uriValidationMap = new HashMap<>();

        geneticResourceModels.forEach(geneticResourceModel -> {
            if (geneticResourceModel.getUri() != null && !URIDeserializer.validateURI(geneticResourceModel.getUri().toString())) {
                errors.addError(geneticResourceModel, "Invalid URI format for URI: " + geneticResourceModel.getUri().toString());
            }
            if (!validateUriUsingMap(geneticResourceModel.getType(), uriValidationMap) ) {
                errors.addError(geneticResourceModel, getInvalidUriFormatOrNullErrorMessage(geneticResourceModel.getType(), "GeneticResource type"));
            }
            if (geneticResourceModel.getLabel() == null || geneticResourceModel.getName().isBlank()) {
                errors.addError(geneticResourceModel, "GeneticResource name is mandatory");
            }
            if (geneticResourceModel.getWebsite() != null && !URIDeserializer.validateURI(geneticResourceModel.getWebsite().toString())) {
                errors.addError(geneticResourceModel, "Invalid URI format for website: " + geneticResourceModel.getWebsite().toString());
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
     * @param geneticResourceModels to check if they are not already in the database
     * @param errors map in which to put the errors
     */
    private void lookForAlreadyExistantUri(
                    List<GeneticResourceModel> geneticResourceModels,
                    MultipleErrorObjectList<MultipleCreateUpdateErrorObject,
                    GeneticResourceModel> errors) throws Exception {

        Collection<URI> existingUris = getNonExistingUris(geneticResourceModels.stream()
                .map(SPARQLResourceModel::getUri)
                .toList());

        if (existingUris.isEmpty()) {
            return;
        }

        geneticResourceModels.forEach(geneticResourceModel -> {
            if (SPARQLDeserializers.containsURI(existingUris, geneticResourceModel.getUri())) {
                errors.addError(geneticResourceModel, "GeneticResource URI already exists, it cannot be created again.");
            }
        });
    }

    /**
     * @param geneticResourceModels to check if their types exist in the database (basically if they are species, variety or accession)
     * @param errors map in which to put the errors. Error format : key = geneticResource URI, value = error message (explaining which type doesn't exist)
     */
    private void validateTypes(List<GeneticResourceModel> geneticResourceModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors) {
        Set<URI> uniqueTypes = geneticResourceModels.stream()
                .map(GeneticResourceModel::getType)
                .collect(Collectors.toSet());

        Set<URI> nonExistingTypes = new HashSet<>();
        for (URI type : uniqueTypes) {
            boolean isType = cacheType.get(new GeneticResourceLogic.KeyType(type), this::checkType);
            if (!isType) {
                nonExistingTypes.add(type);
            }
        }

        if (!nonExistingTypes.isEmpty()) {
            Map<URI, List<GeneticResourceModel>> geneticResourcesErrorByType = new HashMap<>();
            nonExistingTypes.forEach(type -> geneticResourcesErrorByType.put(type, new ArrayList<>()));
            geneticResourceModels.forEach(geneticResourceModel -> {
                if (nonExistingTypes.contains(geneticResourceModel.getType())) {
                    geneticResourcesErrorByType.get(geneticResourceModel.getType()).add(geneticResourceModel);
                }
            });

            geneticResourcesErrorByType.forEach((type, uris) -> uris.forEach(geneticResource -> errors.addError(geneticResource, type+" : rdfType doesn't exist in the ontology")));
        }
    }

    /**
     * validate that every accession, variety or species, that one (or many) geneticResource depend on, exist in the database and has the right type, and the URI is valid.
     */
    private void validateGeneticResourceDependenciesExists(List<GeneticResourceModel> geneticResourceModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors) throws SPARQLException, DisplayableResponseException {

        //list every URI by type, type being species, variety or accession only if uri is valid
        Map<Resource, Set<URI>> urisByType = Map.of(Oeso.Species, new HashSet<>(), Oeso.Variety, new HashSet<>(), Oeso.Accession, new HashSet<>());
        Map<Resource, Map<URI, Boolean>> uriValidationMap = Map.of(Oeso.Species, new HashMap<>(), Oeso.Variety, new HashMap<>(), Oeso.Accession, new HashMap<>());

        geneticResourceModels.forEach(geneticResourceModel -> {
            if (geneticResourceModel.getSpecies() != null) {
                if (validateUriUsingMap(geneticResourceModel.getSpecies().getUri(), uriValidationMap.get(Oeso.Species))) {
                    urisByType.get(Oeso.Species).add(geneticResourceModel.getSpecies().getUri());
                }
                else errors.addError(geneticResourceModel,
                        getInvalidUriFormatOrNullErrorMessage(geneticResourceModel.getSpecies().getUri(), "Species"));
            }
            if (geneticResourceModel.getVariety() != null) {
                if (validateUriUsingMap(geneticResourceModel.getVariety().getUri(), uriValidationMap.get(Oeso.Variety))) {
                    urisByType.get(Oeso.Variety).add(geneticResourceModel.getVariety().getUri());
                }
                else errors.addError(geneticResourceModel,
                        getInvalidUriFormatOrNullErrorMessage(geneticResourceModel.getVariety().getUri(), "Variety"));
            }
            if (geneticResourceModel.getAccession() != null) {
                if (validateUriUsingMap(geneticResourceModel.getAccession().getUri(), uriValidationMap.get(Oeso.Accession))) {
                    urisByType.get(Oeso.Accession).add(geneticResourceModel.getAccession().getUri());
                }
                else errors.addError(geneticResourceModel,
                        getInvalidUriFormatOrNullErrorMessage(geneticResourceModel.getAccession().getUri(), "Accession"));
            }
        });

        //list every URI that doesn't exist in the database with the right type
        Map<Resource, List<URI>> uriThatDoesntExistWithThisType = new HashMap<>();
        for (Map.Entry<Resource, Set<URI>> entry : urisByType.entrySet()) {
            checkUrisExistsWithType(entry.getKey(), entry.getValue(), uriThatDoesntExistWithThisType);
        }

        // fill the errors map
        if (!uriThatDoesntExistWithThisType.isEmpty()) {
            geneticResourceModels.forEach(geneticResourceModel -> {
                if (geneticResourceModel.getSpecies() != null) {
                    var specieUri = geneticResourceModel.getSpecies().getUri();
                    var typeToCheck = Oeso.Species;
                    var nonExistingURI = uriThatDoesntExistWithThisType.get(typeToCheck);
                    addErrorIfUriDoesntExistForDependency(geneticResourceModel, typeToCheck, specieUri, nonExistingURI, errors);
                }
                if (geneticResourceModel.getVariety() != null) {
                    var varietyUri = geneticResourceModel.getVariety().getUri();
                    var typeToCheck = Oeso.Variety;
                    var nonExistingURI = uriThatDoesntExistWithThisType.get(typeToCheck);
                    addErrorIfUriDoesntExistForDependency(geneticResourceModel, typeToCheck, varietyUri, nonExistingURI, errors);
                }
                if (geneticResourceModel.getAccession() != null) {
                    var accessionUri = geneticResourceModel.getAccession().getUri();
                    var typeToCheck = Oeso.Accession;
                    var nonExistingURI = uriThatDoesntExistWithThisType.get(typeToCheck);
                    addErrorIfUriDoesntExistForDependency(geneticResourceModel, typeToCheck, accessionUri, nonExistingURI, errors);
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

    private void addErrorIfUriDoesntExistForDependency(GeneticResourceModel geneticResource, Resource type, URI dependencyUri, List<URI>notExistingURI, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors) {
        if (notExistingURI != null && SPARQLDeserializers.containsURI(notExistingURI, dependencyUri)) {
            errors.addError(geneticResource, String.format("no %s found with this uri : %s .", type.getLocalName(), dependencyUri));
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
    private void validateAccessionVarietyOrSpeciesAreGiven(List<GeneticResourceModel> geneticResourceModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors) throws DisplayableResponseException {
        Map<String, String> messages = Map.of(
                SPARQLDeserializers.getExpandedURI(Oeso.Variety.getURI()), "species",
                SPARQLDeserializers.getExpandedURI(Oeso.Accession.getURI()), "variety or species"
        );
        String missingLinkMessage = "you have to fill %s for this germplam";

        for (GeneticResourceModel geneticResourceModel : geneticResourceModels) {

            if (SPARQLDeserializers.compareURIs(geneticResourceModel.getType().toString(), Oeso.Species.getURI())) { break; }
            else {
                if (geneticResourceModel.getSpecies() == null && geneticResourceModel.getVariety() == null && geneticResourceModel.getAccession() == null) {
                    String typeOfGeneticResourceMissing = messages.getOrDefault(SPARQLDeserializers.getExpandedURI(geneticResourceModel.getType().toString()),
                            "accession, variety or species");
                    errors.addError(geneticResourceModel, String.format(missingLinkMessage, typeOfGeneticResourceMissing));
                }
            }
        }
    }

    private void checkSpeciesCoherency(List<GeneticResourceModel> geneticResourceModels, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors){
        geneticResourceModels.forEach( geneticResourceModel -> {
            boolean isRelated;
            if (geneticResourceModel.getSpecies() != null && geneticResourceModel.getVariety() != null) {
                //check coherence between variety and species
                isRelated = cache.get(new GeneticResourceCoherencyCacheKey(geneticResourceModel), this::checkVarietyHasSameSpecies);
                if (!isRelated) {
                    String species = geneticResourceModel.getSpecies().getUri() != null ? geneticResourceModel.getSpecies().getUri().toString() : "null";
                    errors.addError(geneticResourceModel, "The given species doesn't match with the given variety. Wrong species : "+species);
                }
            }

            if (geneticResourceModel.getSpecies() != null && geneticResourceModel.getAccession() != null) {
                //check coherence between accession and species
                isRelated = cache.get(new GeneticResourceCoherencyCacheKey(geneticResourceModel), this::checkAccessionHasSameSpecies);
                if (!isRelated) {
                    String species = geneticResourceModel.getSpecies().getUri() != null ? geneticResourceModel.getSpecies().getUri().toString() : "null";
                    errors.addError(geneticResourceModel, "The given species doesn't match with the given accession. Wrong species : "+species);
                }
            }

            if (geneticResourceModel.getVariety() != null && geneticResourceModel.getAccession() != null) {
                //check coherence between variety and accession
                isRelated = cache.get(new GeneticResourceCoherencyCacheKey(geneticResourceModel), this::checkAccessionHasSameVariety);
                if (!isRelated) {
                    String variety = geneticResourceModel.getVariety().getUri() != null ? geneticResourceModel.getVariety().getUri().toString() : "null";
                    errors.addError(geneticResourceModel, "The given variety doesn't match with the given accession. Wrong variety : "+variety);
                }
            }
        });
    }

    /**
     * Set the isUpdate property to true for the models that already exist in the database.
     * @param geneticResourceModels the list of geneticResource models among which some may already exist in the database.
     * @param existingUris the URIs of the geneticResource that already exist in the database.
     * @param multipleErrorObject the list of errors that will be updated with the isUpdate property for the models that already exist.
     */
    private static void setIsUpdateForRelevantModels(List<GeneticResourceModel> geneticResourceModels, Collection<URI> existingUris, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> multipleErrorObject) {
        Collection<GeneticResourceModel> modelsWithExistingUris = geneticResourceModels.stream()
                .filter(geneticResourceModel
                        -> existingUris.stream().anyMatch(existingUri
                            -> geneticResourceModel.getUri() != null && SPARQLDeserializers.compareURIs(geneticResourceModel.getUri(), existingUri)))
                .toList();
        multipleErrorObject.getModelsWithErrorsAsObjects()
                .forEach((modelAsObject, error) -> {
                    if (modelsWithExistingUris.stream().anyMatch( modelThatAlreadyExist ->  modelThatAlreadyExist == modelAsObject)) {
                        error.SetIsUpdate(true);
                    }
                });
    }

    /**
     * Get all experiments associated with a geneticResource
     */
    public ListWithPagination<ExperimentModel> getExpFromGeneticResource(
            AccountModel currentUser,
            URI uri,
            String name,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return dao.getExpFromGeneticResource(currentUser, uri, name, orderByList, page, pageSize);

    }

    /**
     * @return all geneticResource attributes (key). Each attribute is unique
     * */
    public Set<String> getDistinctGeneticResourceAttributes() {
        return dao.getDistinctGeneticResourceAttributes();
    }

    /**
     * @param attribute the attribute key
     * @param attributeValue the attribute search pattern
     * @return each unique attribute values for the given attribute from the geneticResource collection
     * */
    public Set<String> getDistinctGeneticResourceAttributesValues(String attribute, String attributeValue, int page, int pageSize) {
        return dao.getDistinctGeneticResourceAttributesValues(attribute, attributeValue, page, pageSize);
    }

    /**
     * @return the uris of already existing geneticResources. If Uris are not valid, they are ignored.
     */
    public Collection<URI> getNonExistingUris(List<URI> uris) throws Exception {
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
    private void retrieveLinkedSpeciesAndVariety(GeneticResourceModel geneticResourceModel) {
        if (geneticResourceModel.getSpecies() == null && geneticResourceModel.getVariety() != null) {
            GeneticResourceModel variety = cacheGeneticResource.get(new GeneticResourceLogic.KeyGeneticResource(geneticResourceModel.getVariety().getUri()), this::getGeneticResource);
            if (variety != null) {
                geneticResourceModel.setSpecies(variety.getSpecies());
            }

        }
        if (geneticResourceModel.getAccession() != null && ( geneticResourceModel.getVariety() == null || geneticResourceModel.getSpecies() == null) ) {
            GeneticResourceModel accession = cacheGeneticResource.get(new GeneticResourceLogic.KeyGeneticResource(geneticResourceModel.getAccession().getUri()), this::getGeneticResource);
            if (accession != null) {
                if (geneticResourceModel.getVariety() == null) {
                    geneticResourceModel.setVariety(accession.getVariety());
                }
                if (geneticResourceModel.getSpecies() == null) {
                    geneticResourceModel.setSpecies(accession.getSpecies());
                }
            }
        }
    }

    private GeneticResourceModel getGeneticResource(GeneticResourceLogic.KeyGeneticResource key) {
        try {
            return dao.get(key.uri, currentUser, false);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean checkType(GeneticResourceLogic.KeyType key) {
        try {
            return dao.isGeneticResourceType(key.type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check coherency between variety and species
     * @param key is used to store the variety URI and the species URI
     * @return true if the variety has the given species, false otherwise
     */
    private boolean checkVarietyHasSameSpecies(GeneticResourceCoherencyCacheKey key) {
        try {
            GeneticResourceModel variety = dao.get(key.varietyUri, currentUser, false);
            return SPARQLDeserializers.compareURIs(variety.getSpecies().getUri().toString(), key.speciesUri.toString());
        } catch (Exception e) {
            return true; //the variety doesn't exist in the database yet
        }
    }

    /**
     * Check coherency between accession and species
     * @param key is used to store the accession URI and the species URI
     * @return true if the accession has the given species, false otherwise
     */
    private boolean checkAccessionHasSameSpecies(GeneticResourceCoherencyCacheKey key) {
        try {
            GeneticResourceModel accession = dao.get(key.accessionUri, currentUser, false);
            return SPARQLDeserializers.compareURIs(accession.getSpecies().getUri().toString(), key.speciesUri.toString());
        } catch (Exception e) {
            return true; //the accession doesn't exist in the database yet
        }
    }

    /**
     * Check coherency between accession and variety
     * @param key is used to store the accession URI and the variety URI
     * @return true if the accession has the given variety, false otherwise
     */
    private boolean checkAccessionHasSameVariety(GeneticResourceCoherencyCacheKey key) {
        try {
            GeneticResourceModel accession = dao.get(key.accessionUri, currentUser, false);
            return SPARQLDeserializers.compareURIs(accession.getVariety().getUri().toString(), key.varietyUri.toString());
        } catch (Exception e) {
            return true; //the accession doesn't exist in the database yet
        }
    }
    //#endregion

    //#region internal classes

    /**
     * Key for the CAFFEINE cache used to check the coherency between species, variety and accession of a geneticResource
     */
    private static final class GeneticResourceCoherencyCacheKey {

        final URI speciesUri;
        final URI varietyUri;
        final URI accessionUri;

        private GeneticResourceCoherencyCacheKey(GeneticResourceModel geneticResourceModel) {
            speciesUri = geneticResourceModel.getSpecies() == null ? null : geneticResourceModel.getSpecies().getUri();
            varietyUri = geneticResourceModel.getVariety() == null ? null : geneticResourceModel.getVariety().getUri();
            accessionUri = geneticResourceModel.getAccession() == null ? null : geneticResourceModel.getAccession().getUri();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = speciesUri == null ? hash : 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(speciesUri));
            hash = varietyUri == null ? hash : 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(varietyUri));
            hash = accessionUri == null ? hash : 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(accessionUri));
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
            final GeneticResourceCoherencyCacheKey other = (GeneticResourceCoherencyCacheKey) obj;
            if ( compareURIs(speciesUri, other.speciesUri) ) {
                return false;
            }
            if ( compareURIs(varietyUri, other.varietyUri) ) {
                return false;
            }

            return compareURIs(accessionUri, other.accessionUri);
        }

        private boolean compareURIs(URI thisUri, URI otherUri) {
            if (thisUri == null && otherUri == null) {
                return true;
            }
            if (thisUri == null || otherUri == null) {
                return false;
            }
            return SPARQLDeserializers.compareURIs(thisUri.toString(), otherUri.toString());
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
            final GeneticResourceLogic.KeyType other = (GeneticResourceLogic.KeyType) obj;
            return SPARQLDeserializers.compareURIs(this.type.toString(), other.type.toString());
        }
    }

    private static class KeyGeneticResource {

        final URI uri;

        public KeyGeneticResource(URI uri) {
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
            final GeneticResourceLogic.KeyGeneticResource other = (GeneticResourceLogic.KeyGeneticResource) obj;
            return SPARQLDeserializers.compareURIs(this.uri.toString(), other.uri.toString());
        }
    }
    //#endregion
}
