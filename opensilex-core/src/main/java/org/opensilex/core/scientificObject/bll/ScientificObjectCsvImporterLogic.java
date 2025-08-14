package org.opensilex.core.scientificObject.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameListException;
import org.opensilex.core.exception.DuplicateURIListException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvOwlRestrictionValidator;
import org.opensilex.sparql.csv.validation.CsvCellValidationContext;
import org.opensilex.sparql.csv.validation.CustomCsvValidation;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Specialization of {@link AbstractCsvImporter} for {@link Oeso#ScientificObject}. <br>
 * This implementation apply custom validation, batch validation, URI generation and creation, which are inherent to the handling of ScientificObject
 * <br>
 * <br><hr>
 * <b>Custom validations : {@link #addCustomValidation(CustomCsvValidation)}</b>
 * <ul>
 *     <li>In experimental context, any scientific object factor level ({@link Oeso#hasFactor}) must be a factor level of experiment factors. {@link FactorLevelDAO#getLevelsFromExperiment(URI)}</li>
 *     <li>In experimental context, any scientific object facility ({@link Oeso#isHosted}) must be an experiment facility. {@link ExperimentDAO#getAvailableFacilitiesURIs(URI)}</li>
 *     <li>Any scientific object can have a geometry ({@link Oeso#hasGeometry}) which is stored in MongoDB with a {@link GeospatialModel}</li>
 * </ul>
 *
 * <hr>
 * <b>Batch validations : {@link #customBatchValidation(CsvOwlRestrictionValidator, List,int)}</b>
 * <ul>
 *     <li>In experimental context, each object name must be unique inside experiment. We apply {@link ScientificObjectDAO#checkLocalDuplicates(List)} method to ensure that no duplicate names</li>
 *     <li>In experimental context, each URI must unique. We apply {@link ScientificObjectDAO#checkLocalURIDuplicates(List)} method to ensure that no duplicate URIs</li>
 * </ul>
 *
 * <hr>
 * <b>URI generation : {@link #getCheckGeneratedUrisUniquenessQuery(Stream, int)}</b>
 * <ul>
 *     <li>In experimental context, we use the default OS graph to ensure that any OS generated inside a particular experiment/graph, will have a globally unique URI(not only inside experiment)</li>
 * </ul>
 *
 * <hr>
 * <b>Creation : {@link #upsert(CSVValidationModel, List, List)} </b>
 * <ul>
 *     <li>In experimental context, object name and type are copied into global OS graph</li>
 *     <li>In experimental context, experiment species are updated according OS germplasms. {@link ExperimentDAO#updateExperimentSpeciesFromScientificObjects(URI)}</li>
 *     <li>Objects geometries are inserted by using the {@link GeospatialDAO}</li>
 *     <li>If object has a creation date ({@link Oeso#hasCreationDate}) and is hosted in some facility ({@link Oeso#isHosted}), then a corresponding {@link MoveModel} is created by using {@link MoveEventDAO}</li>
 * </ul>
 *
 * @author rcolin
 */
public class ScientificObjectCsvImporterLogic extends AbstractCsvImporter<ScientificObjectModel> {

    private final URI experiment;
    private final GeospatialDAO geoDAO;
    private final MoveEventDAO moveDAO;
    private final ScientificObjectDAO scientificObjectDAO;
    private final ExperimentDAO experimentDAO;
    private ExperimentModel experimentModel;
    private final AccountModel currentUser;

    /**
     * @param sparql     SPARQL service
     * @param mongoDB    MongoDB service (used for move and geospatial handling)
     * @param experiment URI of the experiment
     * @param user       {@link org.opensilex.security.account.dal.AccountModel} used to determine if user has the right to access the experiment {@link ExperimentDAO#validateExperimentAccess(URI, org.opensilex.security.account.dal.AccountModel) }
     */
    public ScientificObjectCsvImporterLogic(SPARQLService sparql, MongoDBService mongoDB, URI experiment, AccountModel user) throws Exception {
        super(
                sparql,
                ScientificObjectModel.class,
                experiment == null ? sparql.getDefaultGraphURI(ScientificObjectModel.class) : experiment,
                ScientificObjectModel::new,
                user.getUri()
        );
        Objects.requireNonNull(user);
        Objects.requireNonNull(mongoDB);

        this.currentUser = user;
        this.experiment = experiment;
        experimentDAO = new ExperimentDAO(sparql, mongoDB);

        geoDAO = new GeospatialDAO(mongoDB);
        moveDAO = new MoveEventDAO(sparql, mongoDB);
        scientificObjectDAO = new ScientificObjectDAO(sparql, mongoDB);

        if (experiment != null) {
            // ensure that the user has the right to access experiment
            // if user has access fetch the experiment
            experimentModel = experimentDAO.get(experiment, user);
            if(experimentModel == null){
                throw new NotFoundURIException("Unknown experiment",experiment);
            }
        }
        addGeometryValidation();
        addIsHostedValidation();
        addFactorLevelValidation();
    }

    private void addGeometryValidation() {

        addCustomValidation(new CustomCsvValidation<>(
                Oeso.hasGeometry.getURI(),
                false,
                (model, value, validator, validationContextSupplier) -> {
                    if (StringUtils.isEmpty(value)) {
                        return;
                    }
                    try {
                        // try to parse from wkt -> Geometry. Collect model into validation metadata
                        Geometry geometry = GeospatialDAO.wktToGeometry(value);

                        // Geometry is collected in order to be inserted by batch inside create() method
                        Map<SPARQLNamedResourceModel,Geometry> geometryMap = (Map<SPARQLNamedResourceModel,Geometry>) validator
                                .getValidationModel()
                                .getObjectsMetadata()
                                .computeIfAbsent(Oeso.hasGeometry.getURI(), isHosted -> new IdentityHashMap<>());

                        geometryMap.put(model,geometry);

                    } catch (JsonProcessingException | ParseException e) {
                        CsvCellValidationContext validationContext = validationContextSupplier.get();
                        validationContext.setMessage(e.getMessage());
                        validator.addInvalidValueError(validationContext);
                    }
                }
        ));
    }

    private void addFactorLevelValidation() throws Exception {

        // no factor level handling outside of experiment
        // add error if a value is set and no experiment
        if (experiment == null) {
            addCustomValidation(new CustomCsvValidation<>(
                    Oeso.hasFactorLevel.getURI(),
                    false, // bypass default validation (redundant since factor level existence and URI parsing is performed here)
                    (model, value, validator, validationContextSupplier) -> {
                        if (!StringUtils.isEmpty(value)) {
                            CsvCellValidationContext validationContext = validationContextSupplier.get();
                            validationContext.setMessage("Can't use factor level outside of experiment context");
                            validator.addInvalidValueError(validationContext);
                        }
                    }));
        } else {
            // factor level usage inside experiment context
            // compute set of factors levels from xp
            final Set<String> uniqueFactorLevels = new FactorLevelDAO(sparql)
                    .getLevelsFromExperiment(experiment)
                    .collect(Collectors.toSet());

            final URI hasFactorLevelURI = URI.create(Oeso.hasFactorLevel.getURI());

            // ensure that only factor levels from experiment factors are used
            addCustomValidation(new CustomCsvValidation<>(
                    Oeso.hasFactorLevel.getURI(),
                    false, // bypass default validation (redundant since factor level existence and URI parsing is performed here)
                    (model, value, validator, validationContextSupplier) -> {

                        if (StringUtils.isEmpty(value)) {
                            return;
                        }
                        String shortValue = URIDeserializer.formatURIAsStr(value);
                        if (!uniqueFactorLevels.contains(shortValue)) {
                            CsvCellValidationContext validationContext = validationContextSupplier.get();
                            validationContext.setMessage("Unknown factor level from experiment factors");
                            validator.addInvalidValueError(validationContext);
                        } else {
                            model.addRelation(experiment, hasFactorLevelURI, URI.class, value);
                        }
                    }
            ));
        }

    }

    private void addIsHostedValidation() throws SPARQLException {


        // no facility handling outside of experiment
        // add error if a value is set and no experiment
        if (experiment == null) {
            addCustomValidation(new CustomCsvValidation<>(
                    Oeso.isHosted.getURI(),
                    false, // bypass default validation (redundant since factor level existence and URI parsing is performed here)
                    (model, value, validator, validationContextSupplier) -> {
                        if (!StringUtils.isEmpty(value)) {
                            CsvCellValidationContext validationContext = validationContextSupplier.get();
                            validationContext.setMessage("Can't use a facility outside of experimental context");
                            validator.addInvalidValueError(validationContext);
                        }
                    }));
        } else {

            // compute set of facilities from xp
            final Set<String> facilities = experimentDAO.getAvailableFacilitiesURIs(experiment)
                    .collect(Collectors.toSet());

            final URI isHostedURI = URI.create(Oeso.isHosted.getURI());

            addCustomValidation(new CustomCsvValidation<>(
                    Oeso.isHosted.getURI(),
                    false, // bypass default validation (redundant since facility existence and URI parsing is performed here)
                    (model, value, validator, validationContextSupplier) -> {

                        if (StringUtils.isEmpty(value)) {
                            return;
                        }
                        String shortValue = URIDeserializer.formatURIAsStr(value);
                        if (!facilities.contains(shortValue)) {
                            CsvCellValidationContext validationContext = validationContextSupplier.get();
                            validationContext.setMessage("Unknown facility from experiment facilities");
                            validator.addInvalidValueError(validationContext);
                        }else {
                            model.addRelation(experiment, isHostedURI, URI.class, value);
                        }

                    }
            ));
        }

    }

    @Override
    protected void handleURIMapping(CsvOwlRestrictionValidator validator, ScientificObjectModel model, int totalRowIdx, List<ScientificObjectModel> modelChunkToCreate, List<ScientificObjectModel> modelChunkToUpdate, Map<String, Integer> generatedUrisToIndexesInChunk, Map<String, Integer> filledUrisToIndexesInChunk, Map<String, Integer> filledUrisToUpdateIndexesInChunk) throws SPARQLException {
        if (checkIfSONameIsNull(validator, model, totalRowIdx)) return;

        // inside an XP
        if (withinExperiment()) {
            // query used to check if a SO with a name already exists in XP
            SPARQLNamedResourceModel alreadyExistingOsWithName = scientificObjectDAO.getUriByNameAndGraph(SPARQLDeserializers.nodeURI(experiment),model.getName());
            if (model.getUri() != null) {
                // check existence of a URI (return false/true) in XP
                List<SPARQLResult> result = scientificObjectDAO.checkUriExistInXP(validator, model, totalRowIdx, rootClassURI, graphNode);
                if (result == null) return;
                String isURIExistInXP = !result.isEmpty() ? result.get(0).getStringValue(SPARQLService.EXISTING_VAR) : "";

                // Scenario 1 & 5: If the URI entered in CSV doesn't exist in XP and there's no SO with the same name in XP -> insert the SO
                if ((isURIExistInXP.equalsIgnoreCase("") || isURIExistInXP.equalsIgnoreCase("false"))
                        && (alreadyExistingOsWithName == null)) {
                    addModelInModelChunk(model, modelChunkToCreate);
                    // register URI to the set of URIs to create new SOs
                    filledUrisToIndexesInChunk.put(model.getUri().toString(), totalRowIdx);
                }

                // Scenario 3: If the URI entered in CSV exist in XP -> update the SO
                else if (isURIExistInXP.equalsIgnoreCase("true")) {
                    addModelInModelChunk(model, modelChunkToUpdate);
                    // register URI to the set of URIs to update the existing SOs
                    filledUrisToUpdateIndexesInChunk.put(model.getUri().toString(), totalRowIdx);
                }

            }
            // Scenario 4: If the URI is empty in CSV and there's a SO with the same name in XP -> update the SO
            else if (model.getUri() == null && alreadyExistingOsWithName != null) {
                // fetching existing SO URI
                URI alreadyExistingOSUri = alreadyExistingOsWithName.getUri();
                model.setUri(alreadyExistingOSUri);
                addModelInModelChunk(model, modelChunkToUpdate);
                // register URI to the set of URIs to create new SOs
                filledUrisToUpdateIndexesInChunk.put(alreadyExistingOSUri.toString(), totalRowIdx);
            }
            // Scenario 2: If the URI is empty in CSV and there's no SO with the same name in XP -> insert the SO
            else if (model.getUri() == null && alreadyExistingOsWithName == null) {
                // register URI to the set of URIs to update the existing SOs
                generateLocallyUniqueUri(model, totalRowIdx, validator.getValidationModel(), generatedUrisToIndexesInChunk);
                addModelInModelChunk(model, modelChunkToCreate);
            }
        }

        // global flow (not inside an XP)
        else {
            super.handleURIMapping(validator, model, totalRowIdx, modelChunkToCreate, modelChunkToUpdate, generatedUrisToIndexesInChunk, filledUrisToIndexesInChunk, filledUrisToUpdateIndexesInChunk);
        }
    }

    private static boolean checkIfSONameIsNull(CsvOwlRestrictionValidator validator, ScientificObjectModel model, int totalRowIdx) {
        // As OS name is mandatory, we are checking if the OS name is empty
        // if it's empty we are showing the 'Missing Required Value' error type
        if(model.getName() == null) {
            String rdfsLabel = URIDeserializer.getShortURI(RDFS.LABEL.stringValue());
            String errorMsg = String.format(ScientificObjectDAO.NO_NAME_ERROR_MSG, model.getUri() == null ? "A new object " : model.getUri().toString());

            CsvCellValidationContext cell = new CsvCellValidationContext(totalRowIdx +CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, AbstractCsvImporter.CSV_NAME_INDEX, model.getName(), rdfsLabel);
            cell.setMessage(errorMsg);
            validator.addMissingRequiredValue(cell);
            return true;
        }
        return false;
    }

    @Override
    protected void checkUrisUniqueness(CsvOwlRestrictionValidator validator, Map<String, Integer> filledUrisToIndexesInChunk, Map<String, Integer> generatedUrisToIndexesInChunk, List<ScientificObjectModel> modelChunk) throws SPARQLException {
        if(withinExperiment()) {
            // check generated uniqueness in batch way
            if(validator.isValid()){
                checkGeneratedUrisUniqueness(generatedUrisToIndexesInChunk, modelChunk, validator);
            }
        }
        else super.checkUrisUniqueness(validator, filledUrisToIndexesInChunk, generatedUrisToIndexesInChunk, modelChunk);
    }

    private boolean withinExperiment() {
        return experiment != null;
    }

    @Override
    protected <T extends SPARQLResourceModel & ClassURIGenerator> void mapObjectsToUpdate(CsvOwlRestrictionValidator validator, List<T> modelChunkToUpdate) {
        // map modelchunkToUpdate List into objectsToUpdate List in validator
        // objectsToUpdate list will be used to call the update method later
        modelChunkToUpdate.forEach(validator.getValidationModel().getObjectsToUpdate()::add);
    }

    /**
     * @apiNote <pre>
     * <b>Experimental context</b>
     * In case of URI generation in experimental context we want to ensure that
     * generated URI are globally unique. So we need to use the global OS graph, since it's contains declaration of any OS
     *
     * <b>Global context</b>
     * Use the default implementation which use the provided graph.
     * Here, since we are outside experimental context, then the provided graph was the default OS graph
     * So, the default behaviour, which use the default OS graph is OK
     * </pre>
     * @see ScientificObjectCsvImporterLogic#getCheckUrisUniquenessQuery(Stream, int)
     */
    @Override
    protected SelectBuilder getCheckGeneratedUrisUniquenessQuery(Stream<String> urisToCheck, int streamSize) {

        if (withinExperiment()) {
            // in case of URI generation in experimental context we want to ensure that
            // generated URI are globally unique. So we need to use the global OS graph, since it's contains declaration of any OS
            return scientificObjectDAO.getCheckUriListExist(urisToCheck, streamSize, rootClassURI);
        } else {
            // use default implementation which use the provided graph.
            // Here, since we are outside experimental context, then the provided graph was the default OS graph
            // So, the default behaviour, which use the default OS graph is OK
            return super.getCheckUrisUniquenessQuery(urisToCheck, streamSize);
        }
    }


    /**
     * @apiNote <pre>
     * Reuse default implementation which use the provided graph.
     * Here, since we are outside experimental context, then the provided graph was the default OS graph
     * So, the default behaviour, which use the default OS graph is OK
     * </pre>
     * @see ScientificObjectCsvImporterLogic#getCheckGeneratedUrisUniquenessQuery(Stream, int)
     */
    @Override
    protected SelectBuilder getCheckUrisUniquenessQuery(Stream<String> urisToCheck, int streamSize) {
        return super.getCheckUrisUniquenessQuery(urisToCheck, streamSize);
    }

    @Override
    protected void customBatchValidation(CsvOwlRestrictionValidator restrictionValidator, List<ScientificObjectModel> modelChunk, int offset) {
        if (experiment != null) {
            // check if there are any duplicate names within CSV
            checkLocalDuplicateNames(restrictionValidator, modelChunk, offset);
            // check if there are any duplicate URIs within CSV
            checkLocalDuplicateURIs(restrictionValidator, modelChunk, offset);
        }
    }

    private void checkLocalDuplicateNames(CsvOwlRestrictionValidator restrictionValidator, List<ScientificObjectModel> modelChunk, int offset) {
        try {
            scientificObjectDAO.checkLocalDuplicates(modelChunk);
        } catch (DuplicateNameListException e) {
            addDuplicateNameErrors(modelChunk, restrictionValidator, e.getExistingUriByName(), offset);
        }
    }
    private void checkLocalDuplicateURIs(CsvOwlRestrictionValidator restrictionValidator, List<ScientificObjectModel> modelChunk, int offset) {
        try {
            scientificObjectDAO.checkLocalURIDuplicates(modelChunk);
        } catch (DuplicateURIListException e) {
            addDuplicateURIErrors(modelChunk, restrictionValidator, e.getExistingNameByURI(), offset);
        }
    }

    private void addDuplicateNameErrors(List<ScientificObjectModel> objects, CsvOwlRestrictionValidator validator, Map<String, URI> existingUriByName, int offset) {

        int i = offset;

        String rdfsLabel = URIDeserializer.getShortURI(RDFS.LABEL.stringValue());

        // iterate object, check if a conflict was found (by name), if so, append an error into validation
        for (ScientificObjectModel object : objects) {
            String name = object.getName();

            if (existingUriByName.containsKey(name)) {
                URI duplicateObjectURI = existingUriByName.get(name);

                // handle case where URI is null (in case of local duplicate with a non set URI)
                String errorMsg = String.format(ScientificObjectDAO.NON_UNIQUE_NAME_ERROR_MSG, name, duplicateObjectURI == null ? "A new object " : duplicateObjectURI.toString());

                CsvCellValidationContext cell = new CsvCellValidationContext(i+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, AbstractCsvImporter.CSV_NAME_INDEX, object.getName(), rdfsLabel);
                cell.setMessage(errorMsg);
                validator.addInvalidValueError(cell);

                if(validator.getNbError() >= errorNbLimit){
                    break;
                }
            }
            i++;
        }
    }

    private void addDuplicateURIErrors(List<ScientificObjectModel> objects, CsvOwlRestrictionValidator validator, Map<URI, String> existingNameByURI, int offset) {

        int i = offset;

        // iterate object, check if a conflict was found (by URI), if so, append an error into validation
        for (ScientificObjectModel object : objects) {
            URI soURI = object.getUri();

            if (existingNameByURI.containsKey(soURI)) {
                String duplicateObjectName = existingNameByURI.get(soURI);

                // handle case where name is null (in case of local duplicate with a non set name)
                String errorMsg = String.format(ScientificObjectDAO.NON_UNIQUE_URI_ERROR_MSG, soURI, duplicateObjectName == null ? "A new object " : duplicateObjectName);

                CsvCellValidationContext cell = new CsvCellValidationContext(i+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, AbstractCsvImporter.CSV_URI_INDEX, object.getUri().toString(), CSV_URI_KEY);
                cell.setMessage(errorMsg);
                validator.addInvalidValueError(cell);

                if(validator.getNbError() >= errorNbLimit){
                    break;
                }
            }
            i++;
        }
    }

    private static void cleanUpGeometryData(CSVValidationModel validation, Map<SPARQLNamedResourceModel, Geometry> objectsGeometry, List<GeospatialModel> geospatialModels) {
        // clear map and values
        objectsGeometry.clear();
        geospatialModels.clear();
        validation.getObjectsMetadata().remove(Oeso.hasGeometry.getURI());
    }

    @NotNull
    private static Set<URI> fetchURIsFromSOModel(List<ScientificObjectModel> models) {
        // fetch URIs from ScientificObjectModel
        return models.stream()
                .map(ScientificObjectModel::getUri)
                .collect(Collectors.toSet());
    }

    @NotNull
    private static List<GeospatialModel> getGeospatialModelList(List<GeospatialModel> geospatialModels, Set<URI> URIsFromModel) {
        // fetch geospatial models which has matching URIs from Model
        return geospatialModels.stream()
                .filter(geoModel -> URIsFromModel.contains(geoModel.getUri()))
                .toList();
    }

    private void setExperimentInSOObj(ScientificObjectModel model) {
        if(experimentModel != null) {
            model.setExperiment(experimentModel);
        }
    }

    /**
     * Check that new factor levels we want to add to the OS are associated to the experiment. Throw an exception if not.
     * experimentModel Experiment model that is (or will be) linked to the OS.
     * @throws InvalidValueException if a factor level is not part of the experiment.
     */
    private void checkFactorLevelsInXP(ScientificObjectModel model) {
        List<URI> experimentFactorLevels = experimentModel.getFactors().stream()
                .flatMap(factor -> factor.getFactorLevels().stream().map(FactorLevelModel::getUri))
                .toList();
        List<URI> descriptionFactorLevels = model.getRelations().stream()
                .filter( relation -> SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.hasFactorLevel.getURI()))
                .map(relation -> {
                    try {
                        return new URI(relation.getValue());
                    } catch (URISyntaxException e) {
                        throw new InvalidValueException("Invalid factor level URI"+ relation.getValue());
                    }
                }).toList();
        descriptionFactorLevels.forEach(factorLevel -> {
            if (!experimentFactorLevels.contains(factorLevel)) {
                throw new InvalidValueException("Following factor level is not part of the experiment: "+factorLevel);
            }
        });
    }

    @Override
    public void upsert(CSVValidationModel validation, List<ScientificObjectModel> modelsToCreate, List<ScientificObjectModel> modelsToUpdate) throws Exception {
        // prepare the geometryMetadatas object to be used later to separate the objects to be created and updated
        Object geometryMetadatas = validation.getObjectsMetadata().get(Oeso.hasGeometry.getURI());
        List<GeospatialModel> geospatialModelsToCreate = new ArrayList<>();
        List<GeospatialModel> geospatialModelsToUpdate = new ArrayList<>();
        List<GeospatialModel> geospatialModels = new ArrayList<>();
        Map<SPARQLNamedResourceModel, Geometry> objectsGeometry = new HashMap<>();

        if (geometryMetadatas != null) {

            objectsGeometry = (Map<SPARQLNamedResourceModel, Geometry>) geometryMetadatas;

            // convert (object,geometry) map into list of GeospatialModel
            geospatialModels = objectsGeometry
                    .entrySet()
                    .stream()
                    .map(entry -> new GeospatialModel(entry.getKey(), graph, entry.getValue()))
                    .collect(Collectors.toList());

            // Step 1: Build a set of URIs from modelToCreateURIs and modelToUpdateURIs
            Set<URI> modelToCreateURIs = fetchURIsFromSOModel(modelsToCreate);
            Set<URI> modelToUpdateURIs = fetchURIsFromSOModel(modelsToUpdate);

            // separate the geospatialModels which are to be created and updated
            geospatialModelsToCreate = getGeospatialModelList(geospatialModels, modelToCreateURIs);
            geospatialModelsToUpdate = getGeospatialModelList(geospatialModels, modelToUpdateURIs);

        }
        // create ScientificObjects: modelsToCreate
        create(modelsToCreate, geospatialModelsToCreate);

        // update ScientificObjects: modelsToUpdate
        update(modelsToUpdate, geospatialModelsToUpdate);

        // cleanup the geometry data from validation, objectsGeometry, geospatialModels after creation & modification
        cleanUpGeometryData(validation, objectsGeometry, geospatialModels);
    }

    private void create(List<ScientificObjectModel> models, List<GeospatialModel> geospatialModelsToCreate) throws Exception {
        if (Objects.nonNull(this.publisher) || experimentModel != null) {
            for (ScientificObjectModel model : models) {
                if (this.publisher != null && Objects.isNull(model.getPublisher())) {
                    model.setPublisher(this.publisher);
                }
                // setting experiment in SO model if we try creating a SO from an XP
                setExperimentInSOObj(model);
            }
        }
        scientificObjectDAO.create(models, graph);

        // associated moves creation
        List<MoveModel> moves = new ArrayList<>();
        for (ScientificObjectModel object : models) {
            MoveModel facilityMoveEvent = new MoveModel();
            if (ScientificObjectDAO.fillFacilityMoveEvent(facilityMoveEvent, object)) {
                moves.add(facilityMoveEvent);
            }
        }
        moveDAO.create(moves);

        // Global OS copy and species update inside xp
        if (withinExperiment()) {
            var soToCreateUriSet = scientificObjectDAO.getExistingUrisToCreate(models);
            if (!soToCreateUriSet.isEmpty()) {
                scientificObjectDAO.copyIntoGlobalGraph(models.stream().filter(model -> {
                    try {
                        // verify if the soToCreateUriSet contains the expanded URI of model
                        // fetching the expandedURI of model here to create the SO with short URI in global graph also
                        return soToCreateUriSet.contains(new URI(SPARQLDeserializers.getExpandedURI(model.getUri())));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            experimentDAO.updateExperimentSpeciesFromScientificObjects(experiment);
        }

        if (!geospatialModelsToCreate.isEmpty()) {
            // Geometry creation into MongoDB
            geoDAO.createAll(geospatialModelsToCreate);
        }
    }

    private void update(List<ScientificObjectModel> models, List<GeospatialModel> geospatialModelsToUpdate) throws Exception {

        GeospatialModel geospatialToBeUpdated;

        // DELETE and INSERT
        for(ScientificObjectModel model : models) {
            // setting experiment in SO model if we try updating a SO from an XP
            setExperimentInSOObj(model);
            scientificObjectDAO.setLastUpdateDateInSO(model);
            Node graphNode = SPARQLDeserializers.nodeURI(experiment);
            List<URI> childrenURIs = scientificObjectDAO.fetchChildrenURIs(model.getUri(), currentUser, graphNode);

            boolean hasFacilityURI = scientificObjectDAO.checkIfSOHasFacilityURIs(model);
            scientificObjectDAO.updateSOAndMove(model.getUri(), currentUser, graphNode, model, childrenURIs, hasFacilityURI);

            if(experiment != null) {
                experimentDAO.updateExperimentSpeciesFromScientificObjects(experiment);
            }

            if(model.getRelations() != null) {
                checkFactorLevelsInXP(model);
            }
            // geospatialModelsToUpdate - all geoSpatialModels for the SOs(which has to be updated)
            // geospatialToBeUpdated - geoSpatialModels to be updated
            geospatialToBeUpdated = geospatialModelsToUpdate.stream()
                    .filter(geospatialModel -> model.getUri().equals(geospatialModel.getUri()))
                    .findFirst()
                    .orElse(null);

            // Geometry modification in MongoDB
            if (geospatialToBeUpdated != null) {
                geoDAO.update(geospatialToBeUpdated, model.getUri(), experiment);
            }
            // Geometry info is deleted from a SO
            else {
                geoDAO.delete(model.getUri(), experiment);
            }
        }
    }

}
