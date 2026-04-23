package org.opensilex.core.scientificObject.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcatDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.api.move.csv.MoveEventCsvImporter;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameListException;
import org.opensilex.core.exception.DuplicateURIListException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.utils.StringUriMap;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvOwlRestrictionValidator;
import org.opensilex.sparql.csv.header.CsvHeader;
import org.opensilex.sparql.csv.validation.CsvCellValidationContext;
import org.opensilex.sparql.csv.validation.CustomCsvValidation;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Specialization of {@link AbstractCsvImporter} for {@link Oeso#ScientificObject}. <br>
 * This implementation apply custom validation, batch validation, URI generation and creation, which are inherent to the handling of ScientificObject
 * <br>
 * <br><hr>
 * <b>Custom validations : {@link #addCustomValidation(CustomCsvValidation)}</b>
 * <ul>
 *     <li>In experimental context, any scientific object factor level ({@link Oeso#hasFactor}) must be a factor level of experiment factors. {@link FactorLevelDAO#getLevelsFromExperiment(URI)}</li>
 * </ul>
 *
 * <hr>
 * <b>Batch validations : {@link #customBatchValidation(CsvOwlRestrictionValidator, List, int, boolean)}</b>
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
 * </ul>
 *
 * @author rcolin
 */
public class ScientificObjectCsvImporterLogic extends AbstractCsvImporter<ScientificObjectModel> {

    private final URI experiment;
    private final MoveLogic moveLogic;
    private final ScientificObjectDAO scientificObjectDAO;
    private final ExperimentDAO experimentDAO;
    private ExperimentModel experimentModel;
    private final AccountModel currentUser;
    private final ScientificObjectLogic scientificObjectLogic;
    private final LocationObservationLogic locationObservationLogic;
    private final LocationObservationCollectionLogic locationObservationCollectionLogic;

    private static String EXPERIMENTS_CONCAT_VAR_NAME = "experiments";
    private static final String MOVES_TO_UPDATE_INFORMATION_CACHEKEY = "MOVES_TO_UPDATE_INFORMATION";
    private static final String GEOMETRY_STUFF_METADATA_KEY = "GEOMETRY_INFORMATION";
    public static final String MOVE_START_FIELD_UNIQUE_HEADER = "start_date_of_Location";
    public static final String MOVE_END_FIELD_UNIQUE_HEADER = "end_date_of_Location";
    public static final int LOCATION_FETCHING_PAGE_SIZE = 10000;

    /**
     * The extra move columns we expect. Some things from Events (like isInstant, target...) can be deduced so no need for those columns.
     */
    public static final Set<String> extraColumns = Stream.concat(
            Stream.of(
                    MOVE_START_FIELD_UNIQUE_HEADER,
                    MOVE_END_FIELD_UNIQUE_HEADER
            ),
            MoveEventCsvImporter.MOVE_SPECIFIC_HEADERS.stream()
    ).collect(Collectors.toSet());

    //Store a MoveModel, and the corresponding line index, as we are making our way through a row. Once a new row is reached we
    //can add to the movePerScientificObject map and then reset the currentMoveModel
    private MoveModel currentMoveModel = new MoveModel();
    //Boolean to keep track of if any move field was filled in for this line. This only counts the move specific fields and the event start and end dates
    private boolean atLeast1MoveFieldFilledForCurrentRow = false;

    /**
     * @param sparql     SPARQL service
     * @param mongoDB    MongoDB service (used for move and geospatial handling)
     * @param experiment URI of the experiment
     * @param user       {@link org.opensilex.security.account.dal.AccountModel} used to determine if user has the right to access the experiment {@link ExperimentDAO#validateExperimentAccess(URI, org.opensilex.security.account.dal.AccountModel) }
     */
    public ScientificObjectCsvImporterLogic(
            SPARQLService sparql,
            MongoDBService mongoDB,
            URI experiment,
            AccountModel user,
            FileStorageService fs,
            ClientSession session
    ) throws Exception {
        super(
                sparql,
                ScientificObjectModel.class,
                experiment == null ? sparql.getDefaultGraphURI(ScientificObjectModel.class) : experiment,
                ScientificObjectModel::new,
                user.getUri(),
                extraColumns
        );
        Objects.requireNonNull(user);
        Objects.requireNonNull(mongoDB);

        this.currentUser = user;
        this.experiment = experiment;
        experimentDAO = new ExperimentDAO(sparql, mongoDB);

        moveLogic = new MoveLogic(sparql, mongoDB, user, session);
        this.locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);
        this.locationObservationLogic = new LocationObservationLogic(mongoDB.getServiceV2(), sparql);
        scientificObjectDAO = new ScientificObjectDAO(sparql);
        this.scientificObjectLogic = new ScientificObjectLogic(sparql, mongoDB, fs);


        if (experiment != null) {
            // ensure that the user has the right to access experiment
            // if user has access fetch the experiment
            experimentModel = experimentDAO.get(experiment, user);
            if(experimentModel == null){
                throw new NotFoundURIException("Unknown experiment",experiment);
            }
        }
        addFactorLevelValidation();
        addCompatibilityGeometryValidation();
        addCompatibilityIsHostedValidation();
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

    private void addCompatibilityGeometryValidation() {
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
                        Map<SPARQLNamedResourceModel, Geometry> geometryMap = (Map<SPARQLNamedResourceModel, Geometry>) validator
                                .getValidationModel()
                                .getObjectsMetadata()
                                .computeIfAbsent(Oeso.hasGeometry.getURI(), hasGeometry -> new IdentityHashMap<>());

                        geometryMap.put(model, geometry);

                    } catch (JsonProcessingException | ParseException e) {
                        CsvCellValidationContext validationContext = validationContextSupplier.get();
                        validationContext.setMessage(e.getMessage());
                        validator.addInvalidValueError(validationContext);
                    }
                }
        ));
    }

    private void addCompatibilityIsHostedValidation() throws SPARQLException {
        // no facility handling outside of experiment
        // add error if a value is set and no experiment
        if (experiment == null) {
            addCustomValidation(new CustomCsvValidation<>(
                    Oeso.isHosted.getURI(),
                    false, // bypass default validation (redundant since factor level existence and URI parsing is performed here)
                    (model, value, validator, validationContextSupplier) -> {
                        if (!StringUtils.isEmpty(value)) {
                            CsvCellValidationContext validationContext = validationContextSupplier.get();
                            validationContext.setMessage("vocabulary:isHosted cannot be used outside of experiment context");
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
                        }  else {
                            // Geometry is collected in order to be inserted by batch inside create() method
                            Map<SPARQLNamedResourceModel, URI> isHostedMap = (Map<SPARQLNamedResourceModel, URI>) validator
                                    .getValidationModel()
                                    .getObjectsMetadata()
                                    .computeIfAbsent(Oeso.isHosted.getURI(), isHosted -> new IdentityHashMap<>());
                            isHostedMap.put(model, URI.create(value));
                        }
                    }
            ));
        }
    }

    @Override
    protected boolean handleURIMapping(
            CsvOwlRestrictionValidator validator,
            ScientificObjectModel model,
            int rowIndex,
            List<ScientificObjectModel> modelChunkToCreate,
            List<ScientificObjectModel> modelChunkToUpdate,
            Map<String, Integer> generatedUrisToIndexesInChunk,
            Map<String, Integer> filledUrisToIndexesInChunk
    ) throws SPARQLException {
        if (checkIfSONameIsNull(validator, model, rowIndex)) return false;

        SPARQLNamedResourceModel alreadyExistingOsWithName = null;

        // inside an XP
        if (withinExperiment()) {
            // query used to check if a SO with a name already exists in XP
            //Only look if in experiment as duplicate names can exist globally
            alreadyExistingOsWithName = scientificObjectLogic.getUriByNameAndGraph(
                    SPARQLDeserializers.nodeURI(experiment),
                    model.getName()
            );
        }

        if (model.getUri() != null) {
            // check existence of a URI (return false/true) in Context
            List<SPARQLResult> result = scientificObjectDAO.checkUriExistInContext(validator, model, rowIndex, rootClassURI, graphNode);

            if (result == null) return false;
            String isURIExistInGraphString = !result.isEmpty() ? result.get(0).getStringValue(SPARQLService.EXISTING_VAR) : "";
            boolean isUriExistInGraph = isURIExistInGraphString.equalsIgnoreCase("true");

            // Scenario 1 & 5: If the URI entered in CSV doesn't exist and if there's no SO with the same name in XP if we are in an XP -> insert the SO
            if (!isUriExistInGraph && (alreadyExistingOsWithName == null)) {
                modelChunkToCreate.add(model);
                // register URI to the set of URIs to create new SOs
                filledUrisToIndexesInChunk.put(model.getUri().toString(), rowIndex);
                return false;
            }

            // Scenario 3: If the URI entered in CSV does exist in context -> update the SO
            else if (isUriExistInGraph) {
                modelChunkToUpdate.add(model);
                return true;
            }
        }
        // Scenario 4: If the URI is empty in CSV and there's a SO with the same name in XP -> update the SO
        else if (model.getUri() == null && alreadyExistingOsWithName != null) {
            // fetching existing SO URI
            URI alreadyExistingOSUri = alreadyExistingOsWithName.getUri();
            model.setUri(alreadyExistingOSUri);
            modelChunkToUpdate.add(model);
            return true;
        }
        // Scenario 2: If the URI is empty in CSV and there's no SO with the same name in XP -> insert the SO
        else {
            // register URI to the set of URIs to update the existing SOs
            generateLocallyUniqueUri(model, rowIndex, validator.getValidationModel(), generatedUrisToIndexesInChunk);
            modelChunkToCreate.add(model);
            return false;
        }
        return false;
    }



    @Override
    protected void readExtraStringColumn(
            String columnTitle,
            String value,
            int rowIdx,
            ScientificObjectModel sciObjModel,
            int colIndex,
            CsvOwlRestrictionValidator restrictionValidator
    ) throws Exception{
        //The extra columns we've defined correspond to move stuff
        if(columnTitle.equals(MOVE_START_FIELD_UNIQUE_HEADER)){
            try{
                moveLogic.applyValueOnStartField(value, currentMoveModel);
            } catch(BadRequestException | DateTimeParseException e){
                CsvCellValidationContext cell = new CsvCellValidationContext(rowIdx, colIndex, value, EventModel.START_FIELD);
                cell.setMessage(e.getMessage());
                restrictionValidator.addInvalidDateError(cell);
            }
            return;
        }
        if(columnTitle.equals(MOVE_END_FIELD_UNIQUE_HEADER)){
            try{
                moveLogic.applyValueOnEndField(value, currentMoveModel);
            } catch(BadRequestException | DateTimeParseException e){
                CsvCellValidationContext cell = new CsvCellValidationContext(rowIdx, colIndex, value, EventModel.END_FIELD);
                cell.setMessage(e.getMessage());
                restrictionValidator.addInvalidDateError(cell);
            }
            return;
        }
        LocationModel movesLocation = MoveLogic.getOrCreateMovesLocationObservation(currentMoveModel, experiment);
        if(columnTitle.equals(LocationModel.FROM_FIELD)){
            try {
                boolean valueWasNotNull = moveLogic.applyValueOnFromField(value, movesLocation);
                atLeast1MoveFieldFilledForCurrentRow = valueWasNotNull || atLeast1MoveFieldFilledForCurrentRow;
            } catch (URISyntaxException e) {
                CsvCellValidationContext cell = new CsvCellValidationContext(rowIdx, colIndex, value, LocationModel.FROM_FIELD);
                cell.setMessage("There was some problem during the parsing of 'from' field.");
                restrictionValidator.addInvalidURIError(cell);
            }
            return;
        }
        if(columnTitle.equals(LocationModel.TO_FIELD)){
            try {
                boolean valueWasNotNull = moveLogic.applyValueOnTooField(value, movesLocation);
                atLeast1MoveFieldFilledForCurrentRow = valueWasNotNull || atLeast1MoveFieldFilledForCurrentRow;
            } catch (URISyntaxException e) {
                CsvCellValidationContext cell = new CsvCellValidationContext(rowIdx, colIndex, value, LocationModel.TO_FIELD);
                cell.setMessage("Not a valid URI for too field");
                restrictionValidator.addInvalidURIError(cell);
            }
            return;
        }
        if(columnTitle.equals(LocationModel.GEOMETRY_FIELD)){
            try{
                boolean valueWasNotNull = moveLogic.applyValueOnGeometryField(value, movesLocation);
                atLeast1MoveFieldFilledForCurrentRow = valueWasNotNull || atLeast1MoveFieldFilledForCurrentRow;
            } catch (ParseException | JsonProcessingException e){
                CsvCellValidationContext cell = new CsvCellValidationContext(rowIdx,colIndex, value,LocationModel.GEOMETRY_FIELD);
                restrictionValidator.addInvalidValueError(cell);
            }
            return;
        }
        if(columnTitle.equals(LocationModel.X_FIELD)){
            boolean valueWasNotNull = moveLogic.applyValueOnXField(value, movesLocation);
            atLeast1MoveFieldFilledForCurrentRow = valueWasNotNull || atLeast1MoveFieldFilledForCurrentRow;
            return;
        }
        if(columnTitle.equals(LocationModel.Y_FIELD)){
            boolean valueWasNotNull = moveLogic.applyValueOnYField(value, movesLocation);
            atLeast1MoveFieldFilledForCurrentRow = atLeast1MoveFieldFilledForCurrentRow || valueWasNotNull;
            return;
        }
        if(columnTitle.equals(LocationModel.Z_FIELD)){
            boolean valueWasNotNull = moveLogic.applyValueOnZField(value, movesLocation);
            atLeast1MoveFieldFilledForCurrentRow = atLeast1MoveFieldFilledForCurrentRow || valueWasNotNull;
            return;
        }
        if(columnTitle.equals(LocationModel.TEXTUAL_POSITION_FIELD)){
            boolean valueWasNotNull = moveLogic.applyValueOnTextualField(value, movesLocation);
            atLeast1MoveFieldFilledForCurrentRow = atLeast1MoveFieldFilledForCurrentRow || valueWasNotNull;
        }
    }

    /**
     * Performs some operations once we've finished reading a row. The operations all involve MOVES:
     * Checking if there were any Move headers/columns present, if there were none we quit the function immediately
     * Checking if any move fields were filled, only does next operations if so.
     * Checking that both move start and end dates aren't null, add an error cell if so.
     * If the dates weren't both null, then we can automatically set the Moves isInstant field.
     * Sets the type of Event top Oeev.Move.
     * Puts the fabricated move into the ScientificObjectUri -> MoveModel Map.
     * Initializes the next MoveModel.
     *
     * Some of this stuff isn't in the MoveLogic class as we are in a specific case here, automatically setting isInstant and things.
     *
     * @param rowIdx the current row index (0 is first row of body, not the header line)
     * @param sciObjModel the ScientificObject that this row produced
     * @param restrictionValidator to toss some errors onto the CSV when needed
     * @param header Contains all the information of the csv header, including the important , were there any Location columns question.
     */
    @Override
    protected void performEndOfRowOperations(int rowIdx, ScientificObjectModel sciObjModel, CsvOwlRestrictionValidator restrictionValidator, CsvHeader header, boolean isForUpdate){
        StringUriMap<MoveModel> movePerScientificObjectUri = (StringUriMap<MoveModel>) restrictionValidator
                .getValidationModel()
                .getObjectsMetadata()
                .computeIfAbsent(GEOMETRY_STUFF_METADATA_KEY, n -> new StringUriMap<MoveModel>());

        var geometryMapCompat = (Map<SPARQLNamedResourceModel, Geometry>) restrictionValidator
                .getValidationModel()
                .getObjectsMetadata()
                .getOrDefault(Oeso.hasGeometry.getURI(), Collections.emptyMap());

        var facilityMapCompat = (Map<SPARQLNamedResourceModel, URI>) restrictionValidator
                .getValidationModel()
                .getObjectsMetadata()
                .getOrDefault(Oeso.isHosted.getURI(), Collections.emptyMap());

        if (geometryMapCompat.containsKey(sciObjModel) || facilityMapCompat.containsKey(sciObjModel)) {
            if (isForUpdate) {
                var cell = new CsvCellValidationContext(rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, header.size() - 1, "", Oeso.hasGeometry.getURI());
                cell.setMessage("vocabulary:hasGeometry and vocabulary:isHosted cannot be used for an update. Please update the object location using a Move event.");
                restrictionValidator.addInvalidValueError(cell);
                atLeast1MoveFieldFilledForCurrentRow = false;
                currentMoveModel = new MoveModel();
                return;
            }
            if (atLeast1MoveFieldFilledForCurrentRow) {
                var cell = new CsvCellValidationContext(rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET, header.size() - 1, "", Oeso.hasGeometry.getURI());
                cell.setMessage("vocabulary:hasGeometry is a compatibility field. It cannot be used along with the Move event model. vocabulary:hasGeometry will be removed in the future ; please only use the Move event model.");
                restrictionValidator.addInvalidValueError(cell);
                atLeast1MoveFieldFilledForCurrentRow = false;
                currentMoveModel = new MoveModel();
                return;
            }
            var creationDate = Optional.ofNullable(sciObjModel.getRelation(Oeso.hasCreationDate)).map(rel -> LocalDate.parse(rel.getValue())).orElse(null);
            var moveCompat = scientificObjectLogic.getCompatibilityMoveModel(experiment, creationDate, geometryMapCompat.get(sciObjModel), facilityMapCompat.get(sciObjModel));
            moveCompat.setTargets(Collections.singletonList(sciObjModel.getUri()));
            movePerScientificObjectUri.put(sciObjModel.getUri(), moveCompat);
            return;
        }

        //All the end of row operations at the time of writing this are about Moves, so quit function if there were no Move columns
        if(!restrictionValidator.getValidationModel().getCsvHeader().doesContainExtraStringColumns()){
            return;
        }

        //If at least one move field was filled then perform some last operations on it, if nay then leave this function
        if (!atLeast1MoveFieldFilledForCurrentRow) {
            //Before leaving function, add an error if no Location specific field filled, but we did fill start and end dates
            //This validation is specific to moves with OS's, in the direct Move importer, any location field being null simply creates an error, whiles here it is
            //possible to have no move with an OS.
            if (currentMoveModel.getStart() != null || currentMoveModel.getEnd() != null) {
                CsvCellValidationContext cell = new CsvCellValidationContext(
                        rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                        header.size()-1,
                        "",
                        "from, to, x, y, z or geometry"
                );
                cell.setMessage("A Location must have at least one non-date field filled.");
                restrictionValidator.addMissingRequiredValue(cell);
            }
            return;
        }
        //From and too validation, null pointer exception on location isn't possible as we would have quit function in previous if block
        LocationModel currentMovesLocation = currentMoveModel.getLocationObservation().getLocation();
        String potentialFromTooErrorMsg = LocationLogic.validateFromAndTooValuesAndReturnErrorMsg(
                currentMovesLocation.getFrom(),
                currentMovesLocation.getTo()
        );
        if(potentialFromTooErrorMsg != null){
            CsvCellValidationContext cell = new CsvCellValidationContext(
                    rowIdx,
                    CollectionUtils.isEmpty(header.getIndexes(LocationModel.TO_FIELD)) ? header.size()-1 : header.getIndexes(LocationModel.TO_FIELD).get(0),
                    currentMovesLocation.getTo().toString(),
                    "To"
            );
            cell.setMessage(potentialFromTooErrorMsg);
            if(potentialFromTooErrorMsg.equals(LocationLogic.FROM_BUT_NO_TOO_ERROR_MSG)){
                restrictionValidator.addMissingRequiredValue(cell);
            }else {
                restrictionValidator.addInvalidValueError(cell);
            }
        }
        //No start or end for move validation, haven't reformatted as the similar validations in MoveEventCsvImporter depend on isInstant
        if(currentMoveModel.getStart() == null && currentMoveModel.getEnd() == null){
            //Try to create an error missing cell on the corresponding end column, if this column doesn't exist just toss it at the end of the line.
            CsvCellValidationContext cell = new CsvCellValidationContext(
                    rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                    CollectionUtils.isEmpty(header.getIndexes(EventModel.END_FIELD)) ? header.size()-1 : header.getIndexes(EventModel.END_FIELD).get(0),
                    "",
                    EventModel.END_FIELD
            );
            cell.setMessage("A Location must have at least an End date filled");
            restrictionValidator.addMissingRequiredValue(cell);
        }
        //else we can automatically set isInstant
        //First case, we only have an end (only start is null), so it's an Instant
        if(currentMoveModel.getStart() == null){
            currentMoveModel.setIsInstant(true);
        } else {
            //Else either we have just a start, or we have both start and end, either way it's not an instant
            currentMoveModel.setIsInstant(false);
        }

        //Other fields to set automatically
        currentMoveModel.setTargets(Collections.singletonList(sciObjModel.getUri()));
        currentMoveModel.setType(URI.create(Oeev.Move.getURI()));

        //Add Move to the map and initialize next move

        movePerScientificObjectUri.put(sciObjModel.getUri(), currentMoveModel);
        atLeast1MoveFieldFilledForCurrentRow = false;
        currentMoveModel = new MoveModel();
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

    /**
     * Validates that none of the dates in the impacted LocationCollections have any dates that match up.
     * @param newMoveToCreatePerOSUri Map of OS/target URI -> MoveModel that was created from CSV
     */
    private void validateMovesCorrespondingLocationObservationCollectionDates(
            StringUriMap<MoveModel> newMoveToCreatePerOSUri,
            CsvOwlRestrictionValidator restrictionValidator,
            int offset
    ) throws Exception {
        Map<URI, URI>  existingCollectionPerTarget = locationObservationCollectionLogic
                .getLocationObservationCollectionPerFeatureOfInterest(newMoveToCreatePerOSUri.keySet().stream().map(URI::create).toList());

        //Create a list of LocationObservations whose target already exists in a collection, so we can verify no dates overlap
        List<LocationObservationModel> locObsToCheckDatesOfCollectionIn = new ArrayList<>();

        for(var entry : existingCollectionPerTarget.entrySet()){
            MoveModel correspondingMove = newMoveToCreatePerOSUri.get(entry.getKey());
            LocationObservationModel correspondingLocObs = correspondingMove.getLocationObservation();
            //Set The LocationObservation's start and end date from move so we can perform some validations on dates
            correspondingLocObs.setEndDate(correspondingMove.getEnd().getDateTimeStamp().toInstant());
            correspondingLocObs.setStartDate(Objects.nonNull(correspondingMove.getStart()) ? correspondingMove.getStart().getDateTimeStamp().toInstant() : null);
            correspondingLocObs.setObservationCollection(entry.getValue());
            locObsToCheckDatesOfCollectionIn.add(correspondingLocObs);
        }

        //Perform non overlapping dates validation for locations within each LocationObservationCollection encountered
        try{
            locationObservationLogic.validateCollectionsConsistency(locObsToCheckDatesOfCollectionIn);
        }catch (Exception e){
            addMoveRelatedError(restrictionValidator, offset, e.getMessage());
        }
    }

    @Override
    protected void customBatchValidation(
                CsvOwlRestrictionValidator restrictionValidator,
                List<ScientificObjectModel> modelChunk,
                int offset,
                boolean isForUpdate
            ) throws Exception {
        if(CollectionUtils.isEmpty(modelChunk)){
            return;
        }

        //If inside experiment validations
        if (experiment != null) {
            // check if there are any duplicate names within CSV
            checkLocalDuplicateNames(restrictionValidator, modelChunk, offset);
        }

        // check if there are any duplicate URIs within CSV
        checkLocalDuplicateURIs(restrictionValidator, modelChunk, offset);

        StringUriMap<MoveModel> csvFilledMoves = null;
        StringUriMap<MoveModel> movesPerTargetToTestCollectionDatesFor = new StringUriMap<>();

        //Fetch CSV Moves and create a copy, only if there were location columns
        if(restrictionValidator.getValidationModel().getCsvHeader().doesContainExtraStringColumns()){
            csvFilledMoves = (StringUriMap<MoveModel>) restrictionValidator
                    .getValidationModel()
                    .getObjectsMetadata()
                    .get(GEOMETRY_STUFF_METADATA_KEY);

            //Set csvFilledMoves to empty StingUriMap if it is still null so we can still call .get on it in the case
            //where the columns are present but all empty (all deletes)
            if(csvFilledMoves == null){
                csvFilledMoves = new StringUriMap<>();
            }

            //Set movesPerTargetToTestCollectionDatesFor to be ones from csvFilledMoves but only ones that concern this chunk
            //Otherwise we end up checking dates for updatee Moves even though we are supposed to only be checking createe Moves
            for(ScientificObjectModel sciObj : modelChunk){
                MoveModel correspondingCsvMove = csvFilledMoves.get(sciObj.getUri());
                if(correspondingCsvMove != null){
                    movesPerTargetToTestCollectionDatesFor.put(sciObj.getUri(), csvFilledMoves.get(sciObj.getUri()));
                }
            }
        }

        //Validations to perform if batch concerns an update
        if(isForUpdate){
            updatePartOfCustomBatchValidation(
                    restrictionValidator,
                    modelChunk,
                    offset,
                    csvFilledMoves,
                    movesPerTargetToTestCollectionDatesFor
            );
        }
        //Verify that no dates, in any newly created or modified LocationObservations, will have same values as another in same collection
        if(!movesPerTargetToTestCollectionDatesFor.isEmpty()){
            validateMovesCorrespondingLocationObservationCollectionDates(movesPerTargetToTestCollectionDatesFor, restrictionValidator, offset);
        }
    }

    /**
     * Extracted to code to make the readability of some other functions better
     */
    private void handleUpdateValidationWhen1ExistingMoveFound(
            List<MoveModel> existingMoves,
            StringUriMap<MoveModel> existingMoveToUpdatePerTarget,
            MoveModel correspondingMoveFromCSV,
            String currentTarget,
            StringUriMap<MoveModel> movesPerTargetToTestCollectionDatesFor
    ){
        existingMoveToUpdatePerTarget.put(URI.create(currentTarget), existingMoves.get(0));
        //If the dates have not changed between CSVFilledMove and the existing then we should not
        //Check none match in existing collection or else a false error will be flagged
        if(correspondingMoveFromCSV != null &&
                InstantModel.bothInstantsRepresentSameDate(correspondingMoveFromCSV.getEnd(), existingMoves.get(0).getEnd()) &&
                InstantModel.bothInstantsRepresentSameDate(correspondingMoveFromCSV.getStart(), existingMoves.get(0).getStart())
        ){
            movesPerTargetToTestCollectionDatesFor.remove(URI.create(currentTarget));
        }
    }

    /**
     * Extracted to code to make the readability of some other functions better
     */
    private void handleUpdateValidationWhenMultipleExistingMovesFound(
            List<MoveModel> existingMoves,
            StringUriMap<MoveModel> existingMoveToUpdatePerTarget,
            MoveModel correspondingMoveFromCSV,
            String currentTarget,
            StringUriMap<MoveModel> movesPerTargetToTestCollectionDatesFor,
            CsvOwlRestrictionValidator restrictionValidator,
            int offset
    ){
        //Else >1 we need to see which move matches the csv one's dates, start by fetching it
        MoveModel matchedMove = null;
        if(correspondingMoveFromCSV == null){
            //If there is more than 1 existing move, and there was no filled csv one (this normally means delete move),
            //then we don't know which move to delete so  throw error
            try {
                addForbiddenMoveDeleteError(restrictionValidator, offset, currentTarget);
                return;
            } catch (Exception multipleEndDateColumnsError) {
                //Minimal handling of error as we only get error if multiple columns were found for move end date column,
                //which should be handled by header validation
                throw new RuntimeException(multipleEndDateColumnsError);
            }
        }
        //Else a move in CSV exists
        //From here we know we are either getting an error or, an update of location with matching dates
        //Either way we know we don't need to verify if dates of Location are same as an other in same collection
        movesPerTargetToTestCollectionDatesFor.remove(URI.create(currentTarget));

        //See if one of the multiple already existing Moves in this XP has dates that match up with the CSVFilledMove
        for(MoveModel existingMoveModel : existingMoves){
            if(!moveLogic.eventsHaveSameDates(existingMoveModel, correspondingMoveFromCSV)){
                continue;
            }
            //else the event have same dates
            matchedMove = existingMoveModel;
            break;
        }
        if(matchedMove == null){
            //No matching dates were found, and we have multiple moves so we can't update a move
            try {
                addForbiddenMoveUpdateError(restrictionValidator, offset, currentTarget);
                return;
            } catch (Exception multipleEndDateColumnsError) {
                //Minimal handling of error as we only get error if multiple columns were found for move end date column,
                //which should be handled by header validation
                throw new RuntimeException(multipleEndDateColumnsError);
            }
        }
        //Else there is a matched Move
        existingMoveToUpdatePerTarget.put(URI.create(currentTarget), matchedMove);
    }

    /**
     * A function that exists only to enhance readability of the large customBatchValidation function
     */
    private void updatePartOfCustomBatchValidation(
            CsvOwlRestrictionValidator restrictionValidator,
            List<ScientificObjectModel> modelChunk,
            int offset,
            StringUriMap<MoveModel> newMoveToCreatePerOSUri,
            StringUriMap<MoveModel> movesPerTargetToTestCollectionDatesFor

    ) throws Exception {
        //Type changes batch validation
        final int maxNumberOfXpPermittedForTypeChange = experiment != null ? 1 : 0;

        verifyTypeChangeAndApplyConsumer(
                modelChunk,
                (VerifyTypeRequestResult nextParsedResult) -> {
                    if(nextParsedResult.experimentQuantity > maxNumberOfXpPermittedForTypeChange){
                        addForbiddenTypeChangeError(restrictionValidator, offset, nextParsedResult.expandedURI, SPARQLDeserializers.getExpandedURI(nextParsedResult.newTypeUri));
                    }
                },
                true
        );

        //Do Move's validation only if there were Move columns this import
        if(restrictionValidator.getValidationModel().getCsvHeader().doesContainExtraStringColumns()){
            movesUpdatePartOfCustomBatchValidation(
                    restrictionValidator,
                    modelChunk,
                    offset,
                    newMoveToCreatePerOSUri,
                    movesPerTargetToTestCollectionDatesFor
            );
        }
    }

    /**
     * Handles validation of an update of Moves. In function of number of existing moves, and the dates, handles removing of elements from
     * movesPerTargetToTestCollectionDatesFor (we check no crossing over dates within observation collections later, after the calling of this function
     *
     */
    private void movesUpdatePartOfCustomBatchValidation(
            CsvOwlRestrictionValidator restrictionValidator,
            List<ScientificObjectModel> modelChunk,
            int offset,
            StringUriMap<MoveModel> newMoveToCreatePerOSUri,
            StringUriMap<MoveModel> movesPerTargetToTestCollectionDatesFor
    ) throws Exception{
        //Verify there is NOT already more than the one initial move for any OS for whom we are
        //trying to update its move
        StringUriMap<List<MoveModel>> visitedMovesPerTargets = moveLogic.getMovesWithLocationPerTarget(modelChunk.stream().map(SPARQLResourceModel::getUri).toList(), experiment);

        //Map that we will save in cache so we know which Moves to update for each Os short uri
        StringUriMap<MoveModel> existingMoveToUpdatePerTarget = new StringUriMap<>();


        visitedMovesPerTargets.forEach((currentTarget, existingMoves)->{
            MoveModel correspondingMoveFromCSV = newMoveToCreatePerOSUri.get(URI.create(currentTarget));

            if(existingMoves.size() == 1){
                handleUpdateValidationWhen1ExistingMoveFound(
                        existingMoves,
                        existingMoveToUpdatePerTarget,
                        correspondingMoveFromCSV,
                        currentTarget,
                        movesPerTargetToTestCollectionDatesFor
                );
                return;
            }
            //Else there was more than 1 pre-existing move
            handleUpdateValidationWhenMultipleExistingMovesFound(
                    existingMoves,
                    existingMoveToUpdatePerTarget,
                    correspondingMoveFromCSV,
                    currentTarget,
                    movesPerTargetToTestCollectionDatesFor,
                    restrictionValidator,
                    offset
            );
        });

        //Add the Moves to update map to validators metadata so that it gets saved in cache
        restrictionValidator
                .getValidationModel()
                .getObjectsMetadata()
                .put(MOVES_TO_UPDATE_INFORMATION_CACHEKEY, existingMoveToUpdatePerTarget);
    }

    /**
     * Runs a sparql query that will fetch old type of each OS in the chunk, as well as a list of all the experiments that each OS is participating in.
     * If the old type does not match the future type then we know we are trying to update the type of this object, for each one of these we then call the
     * onTypeChange consumer.
     *
     * @param modelChunk each model concerned by the update.
     * @param onTypeChange Function to apply for each model where a type change has been detected.
     * @param doCountExperiments if this is set to false then it simply becomes a "fetch objects where the type has changed" request
     * @throws IOException if something goes wrong during the sparql request
     *
     * Produced/executed request when also fetching experiments:
     *
     * <pre><code>
     *     SELECT  (GROUP_CONCAT(DISTINCT ?experiment ; separator=',') AS ?experiments) ?uri ?rdfType
     * WHERE
     *   { ?rdfType (rdfs:subClassOf)* vocabulary:ScientificObject .
     *     ?uri  rdf:type              ?rdfType ;
     *           vocabulary:participatesIn  ?experiment
     *     VALUES ?uri { <OSURI1><OSURI2>etc }
     *   }
     * GROUP BY ?uri ?rdfType
     * </code></pre>
     */
    private void verifyTypeChangeAndApplyConsumer(
            List<ScientificObjectModel> modelChunk,
            Consumer<VerifyTypeRequestResult> onTypeChange,
            boolean doCountExperiments
    ) throws IOException {
        Map<String, URI> newTypesPerUri = new HashMap<>();
        modelChunk.forEach(model -> newTypesPerUri.put(SPARQLDeserializers.getExpandedURI(model.getUri()), model.getType()));

        try{
            SelectBuilder typesAndExperimentsRequest = createTypeTestRequest(new ArrayList<>(newTypesPerUri.keySet()), doCountExperiments);
            sparql.executeSelectQueryAsStream(typesAndExperimentsRequest).forEach(sparqlResult -> {

                String expandedURI = SPARQLDeserializers.getExpandedURI(sparqlResult.getStringValue(ScientificObjectModel.URI_FIELD));

                String oldTypeUri = SPARQLDeserializers.getExpandedURI(sparqlResult.getStringValue(ScientificObjectModel.TYPE_FIELD));

                //If type is not the same as old model then verify this action is permitted (object must be present in one or fewer experiments)
                URI newTypeUri = newTypesPerUri.get(expandedURI);
                if(!SPARQLDeserializers.compareURIs(oldTypeUri, newTypeUri)){
                    String participatesInStringValue = null;
                    if(doCountExperiments){
                        participatesInStringValue = sparqlResult.getStringValue(EXPERIMENTS_CONCAT_VAR_NAME);
                    }
                    onTypeChange.accept(
                            new VerifyTypeRequestResult(
                                    expandedURI,
                                    newTypeUri,
                                    (StringUtils.isEmpty(participatesInStringValue) ? 0 : participatesInStringValue.split(",").length)
                            )
                    );
                }

            });
        }catch(Exception e){
            throw new IOException("Some problem occurred while fetching types.");
        }
    }

    private SelectBuilder createTypeTestRequest(List<String> osUrisToUpdate, boolean doCountExperiments) throws Exception{
        SelectBuilder select = new SelectBuilder();

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        //Only use following vars if we need to also fetch experiments that each OS is participating in
        Var experimentVar = null;
        Aggregator groupConcat = null;
        Var experimentsConcatVar = null;
        if(doCountExperiments){
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
            experimentVar = makeVar(ScientificObjectModel.PARTICIPATES_IN_FIELD);
            groupConcat = new AggGroupConcatDistinct(exprFactory.asExpr(experimentVar), ",");
            experimentsConcatVar = makeVar(EXPERIMENTS_CONCAT_VAR_NAME);
        }

        SPARQLQueryHelper.addWhereUriValues(select, uriVar.getVarName(), osUrisToUpdate.stream().map(URI::create).toList());
        select.addVar(uriVar);
        select.addVar(typeVar)
                .addWhere(typeVar, Ontology.subClassAny, Oeso.ScientificObject)
                .addWhere(uriVar, RDF.type, typeVar);

        if(doCountExperiments){
            select.addVar(groupConcat.toString(), experimentsConcatVar)
                    .addWhere(uriVar, Oeso.participatesIn, experimentVar);
            select.addGroupBy(uriVar);
            select.addGroupBy(typeVar);
        }

        return select;
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

    private void addForbiddenTypeChangeError(CsvOwlRestrictionValidator validator, int offset, String objectUri, String failedNewTypeUri) {

        String unformattedErrorMessage = this.experiment != null ? ScientificObjectDAO.FORBIDDEN_TYPE_CHANGE_XP_ERROR_MESSAGE : ScientificObjectDAO.FORBIDDEN_TYPE_CHANGE_GLOBAL_ERROR_MESSAGE;
        String errorMsg = String.format(unformattedErrorMessage, objectUri);

        CsvCellValidationContext cell = new CsvCellValidationContext(
                offset+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                CSV_TYPE_INDEX,
                failedNewTypeUri,
                CSV_TYPE_KEY
        );
        cell.setMessage(errorMsg);
        validator.addInvalidValueError(cell);
    }

    private void addForbiddenMoveDeleteError(CsvOwlRestrictionValidator validator, int offset, String target) throws Exception {
        String message = String.format(ScientificObjectLogic.FORBIDDEN_MOVE_DELETE_ERROR_MESSAGE, target);
        addMoveRelatedError(validator, offset, message);
    }

    private void addForbiddenMoveUpdateError(CsvOwlRestrictionValidator validator, int offset, String target) throws Exception {
        String message = String.format(ScientificObjectLogic.FORBIDDEN_MOVE_UPDATE_ERROR_MESSAGE, target);
        addMoveRelatedError(validator, offset, message);
    }

    private void addMoveRelatedError(CsvOwlRestrictionValidator validator, int offset, String message) throws Exception{
        //Just chosen endDate as it seems like the most important field.
        Integer colIndexToPutErrorOn = validator.getValidationModel().getCsvHeader().getIndexOfAUniqueHeader(ScientificObjectCsvImporterLogic.MOVE_END_FIELD_UNIQUE_HEADER);
        if(colIndexToPutErrorOn == null){
            colIndexToPutErrorOn = 0;
        }
        CsvCellValidationContext cell = new CsvCellValidationContext(
                offset+CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                colIndexToPutErrorOn,
                "",
                EventModel.END_FIELD
        );
        cell.setMessage(message);
        validator.addInvalidValueError(cell);
    }

    private static void cleanValidationModel(CSVValidationModel validation) {
        validation.getObjectsMetadata().remove(GEOMETRY_STUFF_METADATA_KEY);
        validation.getObjectsMetadata().remove(MOVES_TO_UPDATE_INFORMATION_CACHEKEY);
        validation.getObjectsMetadata().remove(Oeso.hasGeometry.getURI());
        validation.getObjectsMetadata().remove(Oeso.isHosted.getURI());
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
        //If there were no location columns then we know we don't need to try and fetch anything
        StringUriMap<MoveModel> newMoveToCreatePerOSUri = null;
        StringUriMap<MoveModel> existingMovesToUpdatePerOSUri = null;

        newMoveToCreatePerOSUri = (StringUriMap<MoveModel>) validation
                .getObjectsMetadata()
                .get(GEOMETRY_STUFF_METADATA_KEY);
        existingMovesToUpdatePerOSUri = (StringUriMap<MoveModel>) validation
                .getObjectsMetadata()
                .get(MOVES_TO_UPDATE_INFORMATION_CACHEKEY);

        create(modelsToCreate, newMoveToCreatePerOSUri);
        update(modelsToUpdate, newMoveToCreatePerOSUri, existingMovesToUpdatePerOSUri);

        // cleanup cache
        cleanValidationModel(validation);
    }

    /**
     * Handles actual insertion of Models. Handles insertion of Moves if required. And handles copying of object into
     * global graph if we are in an Experiment
     *
     * @param models the ScientificObjectModels to insert
     * @param movePerScientificObjectUri the moves to insert, fetchable by the corresponding scientific object's uri.
     *                                   Can be null, if this is the case we don't perform any Move operations.
     * @throws Exception
     */
    private void create(List<ScientificObjectModel> models, StringUriMap<MoveModel> movePerScientificObjectUri) throws Exception {

        //Insert OS's before moves so that the moves have a valid target
        if (Objects.nonNull(this.publisher) || experimentModel != null) {
            for (ScientificObjectModel model : models) {
                if (this.publisher != null && Objects.isNull(model.getPublisher())) {
                    model.setPublisher(this.publisher);
                }
                // setting experiment in SO model if we try creating a SO from an XP
                setExperimentInSOObj(model);
            }
        }
        scientificObjectDAO.create(this.graphNode, models);

        //Insert moves if movePerScientificObjectUri is not null
        if(movePerScientificObjectUri != null){
            List<MoveModel> movesToInsert = new ArrayList<>();
            for(ScientificObjectModel osModel : models){
                MoveModel nullableMove = movePerScientificObjectUri.get(osModel.getUri());
                if(nullableMove != null){
                    movesToInsert.add(nullableMove);
                }
            }
            if(!CollectionUtils.isEmpty(movesToInsert)){
                this.moveLogic.createFirstSingleUniqueTargetMoves(movesToInsert);
            }
        }

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
    }

    /**
     * Calculates if a model needs to be updated globally, does this by comparing type to old type.
     * Precondition : this.experiment is not null.
     *
     * @param models Initial models that are being updated
     * @param urisToUpdateGlobally A uri list we will fill with uris of models that do need to also be updated globally.
     * @param modelsToUpdateGloballyByExpandedUri A map for fast retrieval of models later.
     * @throws Exception if the requests fail
     */
    private void calculateModelsToUpdateGlobally(
            List<ScientificObjectModel> models,
            List<URI> urisToUpdateGlobally,
            Map<String, ScientificObjectModel> modelsToUpdateGloballyByExpandedUri
    ) throws Exception {
        verifyTypeChangeAndApplyConsumer(
                models,
                (VerifyTypeRequestResult nextParsedResult) -> {
                    //All we need to do is add each result as the results are each OS where the type has changed
                    urisToUpdateGlobally.add(URI.create(nextParsedResult.expandedURI));
                },
                false
        );

        //Get old versions of global objects we're guna have to update
        List<ScientificObjectModel> modelsToUpdateGlobally = this.scientificObjectLogic.searchByURIs(
                sparql.getDefaultGraphURI(ScientificObjectModel.class),
                urisToUpdateGlobally,
                currentUser,
                false
        );
        modelsToUpdateGlobally.forEach(e->modelsToUpdateGloballyByExpandedUri.put(SPARQLDeserializers.getExpandedURI(e.getUri()), e));
    }

    /**
     * Changes the type and adds any custom relations, unique to the new type, to the global version of Scientific Object
     *
     * @param urisToUpdateGlobally uris of OS's that need to be updated globally, used to identify if this model is concerned.
     * @param modelFromExperiment Experiment's version of the model, used to fetch the new type and unique custom relations.
     * @param modelsToUpdateGloballyByExpandedUri A URI -> model map for fast retrieval of global models
     * @throws Exception
     */
    private void makeChangesToGlobalModel(
            List<URI> urisToUpdateGlobally,
            ScientificObjectModel modelFromExperiment,
            Map<String, ScientificObjectModel> modelsToUpdateGloballyByExpandedUri
    ) throws Exception{
        //If this model wasn't concerned by a type change then return immediately
        if(!urisToUpdateGlobally.stream().anyMatch(uri -> SPARQLDeserializers.compareURIs(uri, modelFromExperiment.getUri()))) {
            return;
        }
        //Create mapper so we can work out which properties are not custom
        SPARQLClassObjectMapper<ScientificObjectModel> mapper = sparql.getMapperIndex().getForClass(ScientificObjectModel.class);
        Set<String> managedProperties = mapper.getClassAnalyzer().getManagedPropertiesUris();
        //Prepare copying of any relations that aren't in managedProperties, and that are not equal to rdfs:comment
        List<SPARQLModelRelation> relationsToAddGlobally = modelFromExperiment.getRelations().stream().filter(
                relation ->
                        !managedProperties.stream().anyMatch(
                                managedProperty -> SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), URI.create(managedProperty))
                        ) && !SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), RDFS.COMMENT.stringValue()) ).toList();
        //Retrieve global model and change its type
        ScientificObjectModel globalModel =  modelsToUpdateGloballyByExpandedUri.get(SPARQLDeserializers.getExpandedURI(modelFromExperiment.getUri()));
        globalModel.setType(modelFromExperiment.getType());
        globalModel.setTypeLabel(modelFromExperiment.getTypeLabel());

        //Add the relations
        for(SPARQLModelRelation relation : relationsToAddGlobally) {
            globalModel.addRelation(
                    sparql.getDefaultGraphURI(ScientificObjectModel.class),
                    URI.create(relation.getProperty().getURI()),
                    relation.getType(),
                    relation.getValue()
            );
        }
    }

    /**
     * Handles updating of ScientificObjects. Handles updating of the global copy in the case of a type-change.
     * Handles update, creation or deletion of Moves if both newMoveToCreatePerOSUri and existingMovesToUpdatePerOSUri
     * are not null.
     *
     * @param models the Scientific Object Models to update
     * @param newMoveToCreatePerOSUri can be null. If not null then it is a StringUriMap allowing us to fetch
     *                                            the new csv versions of Moves for a ScientificObject URI.
     * @param existingMovesToUpdatePerOSUri can be null. If not null then it is a StringUriMap allowing us to fetch
     *      *                                  the pre-existing old versions of Moves for a ScientificObject URI.
     */
    private void update(
            List<ScientificObjectModel> models,
            StringUriMap<MoveModel> newMoveToCreatePerOSUri,
            StringUriMap<MoveModel> existingMovesToUpdatePerOSUri
    ) throws Exception {

        if(CollectionUtils.isEmpty(models)) {
            return;
        }

        List<MoveModel> movesToCreate = new ArrayList<>();
        List<MoveModel> movesToUpdate = new ArrayList<>();
        List<MoveModel> movesToDelete = new ArrayList<>();

        List<URI> urisToUpdateGlobally = new ArrayList<>();
        //Map for faster retrieval of models later
        Map<String, ScientificObjectModel> modelsToUpdateGloballyByExpandedUri = new HashMap<>();

        //Handle preparation of updating in global context after a type change
        if(experiment != null){
            calculateModelsToUpdateGlobally(models, urisToUpdateGlobally, modelsToUpdateGloballyByExpandedUri);
        }

        // DELETE and INSERT
        for(ScientificObjectModel model : models) {
            // setting experiment in SO model if we try updating a SO from an XP
            setExperimentInSOObj(model);

            if(experiment != null) {
                performUpdateOperationsWhenExperimentNonNull(urisToUpdateGlobally, model, modelsToUpdateGloballyByExpandedUri);
            }

            //Handle preparation of Moves updates and deletions only if at least 1 of newMoveToCreatePerOSUri
            //or existingMovesToUpdatePerOSUri is not null
            if(newMoveToCreatePerOSUri != null || existingMovesToUpdatePerOSUri != null){
                prepareUpdateMovesStepForOS(
                        model.getUri(),
                        newMoveToCreatePerOSUri,
                        existingMovesToUpdatePerOSUri,
                        movesToCreate,
                        movesToUpdate,
                        movesToDelete
                );
            }
        }

        //Do the standard update
        scientificObjectLogic.updateMultiple(
                models,
                currentUser,
                experiment==null ? sparql.getDefaultGraph(ScientificObjectModel.class) : SPARQLDeserializers.nodeURI(experiment),
                experiment==null
        );

        //Do the secondary update in global context (if we were updating in an XP and if at least one OS had a type change)
        if(!CollectionUtils.isEmpty(modelsToUpdateGloballyByExpandedUri.entrySet())) {
            scientificObjectLogic.updateMultiple(
                    new ArrayList<>(modelsToUpdateGloballyByExpandedUri.values()),
                    currentUser,
                    sparql.getDefaultGraph(ScientificObjectModel.class),
                    true
            );
        }

        //Perform the Moves operation only if at least 1 of newMoveToCreatePerOSUri
        //or existingMovesToUpdatePerOSUri is not null
        if(newMoveToCreatePerOSUri != null || existingMovesToUpdatePerOSUri != null){
            handleUpdateMovesStep(movesToCreate, movesToUpdate, movesToDelete);
        }
    }

    /**
     *
     *  Some code extracted from the update function to make readability better. Contains any operations that need to be
     *  performed during update of an OS when experiment is not null
     */
    private void performUpdateOperationsWhenExperimentNonNull(
            List<URI> urisToUpdateGlobally,
            ScientificObjectModel model,
            Map<String, ScientificObjectModel> modelsToUpdateGloballyByExpandedUri
    ) throws Exception{
        //Handle updating of species
        experimentDAO.updateExperimentSpeciesFromScientificObjects(experiment);

        //Handle changes to global model if required
        makeChangesToGlobalModel(urisToUpdateGlobally, model, modelsToUpdateGloballyByExpandedUri);

        //Handle anything to do with relations
        if(model.getRelations() != null) {
            //Handle factor levels
            checkFactorLevelsInXP(model);
        }
    }

    private void handleUpdateMovesStep(
            List<MoveModel> movesToCreate,
            List<MoveModel> movesToUpdate,
            List<MoveModel> movesToDelete
    ) throws Exception {
        moveLogic.createFirstSingleUniqueTargetMoves(movesToCreate);
        moveLogic.updateModels(movesToUpdate);
        moveLogic.deleteList(movesToDelete.stream().map(SPARQLResourceModel::getUri).toList());
    }

    /**
     * For the passed OS URI, deduces if the update operation will in reality be either a creation, a deletion, or a real update.
     * Places the model in the correct list for handling all at once after.
     * PRECONDITION : At least one of newMoveToCreatePerOSUri or existingMovesToUpdatePerOSUri is not null
     *
     * @param nextModelUri the next OS URI for whom we want to work out what type of operation we are performing on its Move.
     * @param newMoveToCreatePerOSUri the CSV filled in moves by OS URI
     * @param existingMovesToUpdatePerOSUri the EXISTING moves by OS URI
     * @param movesToCreate List will later be used to create all
     * @param movesToUpdate List will later be used to update all
     * @param movesToDelete List will later be used to delete all
     */
    private void prepareUpdateMovesStepForOS(
            URI nextModelUri,
            StringUriMap<MoveModel> newMoveToCreatePerOSUri,
            StringUriMap<MoveModel> existingMovesToUpdatePerOSUri,
            List<MoveModel> movesToCreate,
            List<MoveModel> movesToUpdate,
            List<MoveModel> movesToDelete
    ){
        //Now that we've arrived in the function we can replace null maps with empty ones so that we don't get null pointer exceptions
        //(If both had been null we never would have entered this function
        newMoveToCreatePerOSUri = newMoveToCreatePerOSUri == null ? new StringUriMap<>() : newMoveToCreatePerOSUri;
        existingMovesToUpdatePerOSUri = existingMovesToUpdatePerOSUri == null ? new StringUriMap<>() : existingMovesToUpdatePerOSUri;

        MoveModel correspondingCsvFilledMove = newMoveToCreatePerOSUri.get(nextModelUri);
        MoveModel existingCorrespondingMove = existingMovesToUpdatePerOSUri.get(nextModelUri);
        if(correspondingCsvFilledMove == null){
            //Case 1 of 3, nothing in csv, so we are either deleting a Move or doing nothing
            if(existingCorrespondingMove != null){
                movesToDelete.add(existingCorrespondingMove);
            }
            return;
        }
        //Else cases where we have a csv filled move
        if(existingCorrespondingMove == null){
            //Case 2 of 3, a move found in csv but non-existing already, so we'll simply be creating
            movesToCreate.add(correspondingCsvFilledMove);
            return;
        }
        //Case 3 of 3, we are updating an existing Move
        correspondingCsvFilledMove.setUri(existingCorrespondingMove.getUri());
        movesToUpdate.add(correspondingCsvFilledMove);
    }

    private static class VerifyTypeRequestResult {
        final String expandedURI;
        final URI newTypeUri;
        final int experimentQuantity;

        private VerifyTypeRequestResult(String expandedURI, URI newTypeUri, int experimentQuantity) {
            this.expandedURI = expandedURI;
            this.newTypeUri = newTypeUri;
            this.experimentQuantity = experimentQuantity;
        }
    }
}
