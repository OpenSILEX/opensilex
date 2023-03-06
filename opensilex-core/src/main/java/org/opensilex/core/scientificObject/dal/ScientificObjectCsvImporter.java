package org.opensilex.core.scientificObject.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameListException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelDAO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvOwlRestrictionValidator;
import org.opensilex.sparql.csv.validation.CsvCellValidationContext;
import org.opensilex.sparql.csv.validation.CustomCsvValidation;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.io.IOException;
import java.net.URI;
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
 *     <li>In experimental context, each object name must unique inside experiment. We apply {@link ScientificObjectDAO#checkUniqueNameByGraph(List, URI)} method to ensure name uniqueness</li>
 * </ul>
 *
 * <hr>
 * <b>URI generation : {@link #getCheckGeneratedUrisUniquenessQuery(Stream, int)}</b>
 * <ul>
 *     <li>In experimental context, we use the default OS graph to ensure that any OS generated inside a particular experiment/graph, will have a globally unique URI(not only inside experiment)</li>
 * </ul>
 *
 * <hr>
 * <b>Creation : {@link #create(CSVValidationModel, List)} </b>
 * <ul>
 *     <li>In experimental context, object name and type are copied into global OS graph</li>
 *     <li>In experimental context, experiment species are updated according OS germplasms. {@link ExperimentDAO#updateExperimentSpeciesFromScientificObjects(URI)}</li>
 *     <li>Objects geometries are inserted by using the {@link GeospatialDAO}</li>
 *     <li>If object has a creation date ({@link Oeso#hasCreationDate}) and is hosted in some facility ({@link Oeso#isHosted}), then a corresponding {@link MoveModel} is created by using {@link MoveEventDAO}</li>
 * </ul>
 *
 * @author rcolin
 */
public class ScientificObjectCsvImporter extends AbstractCsvImporter<ScientificObjectModel> {

    private final URI experiment;
    private final GeospatialDAO geoDAO;
    private final MoveEventDAO moveDAO;
    private final ScientificObjectDAO scientificObjectDAO;
    private final ExperimentDAO experimentDAO;

    /**
     * @param sparql     SPARQL service
     * @param mongoDB    MongoDB service (used for move and geospatial handling)
     * @param experiment URI of the experiment
     * @param user       {@link org.opensilex.security.account.dal.AccountModel} used to determine if user has the right to access the experiment {@link ExperimentDAO#validateExperimentAccess(URI, org.opensilex.security.account.dal.AccountModel) }
     */
    public ScientificObjectCsvImporter(SPARQLService sparql, MongoDBService mongoDB, URI experiment, AccountModel user) throws Exception {
        super(
                sparql,
                ScientificObjectModel.class,
                experiment == null ? sparql.getDefaultGraphURI(ScientificObjectModel.class) : experiment,
                ScientificObjectModel::new
        );
        Objects.requireNonNull(mongoDB);
        Objects.requireNonNull(user);

        this.experiment = experiment;
        experimentDAO = new ExperimentDAO(sparql, mongoDB);

        geoDAO = new GeospatialDAO(mongoDB);
        moveDAO = new MoveEventDAO(sparql, mongoDB);
        scientificObjectDAO = new ScientificObjectDAO(sparql, mongoDB);

        if (experiment != null) {
            // ensure that the user has the right to access experiment
            experimentDAO.validateExperimentAccess(experiment, user);
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

    private boolean withinExperiment() {
        return experiment != null;
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
     * @see ScientificObjectCsvImporter#getCheckUrisUniquenessQuery(Stream, int)
     */
    @Override
    protected SelectBuilder getCheckGeneratedUrisUniquenessQuery(Stream<String> urisToCheck, int streamSize) {

        if (withinExperiment()) {
            // in case of URI generation in experimental context we want to ensure that
            // generated URI are globally unique. So we need to use the global OS graph, since it's contains declaration of any OS
            return sparql.getCheckUriListExistQuery(urisToCheck, streamSize, rootClassURI.toString(), scientificObjectDAO.getDefaultGraphNode());
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
     * @see ScientificObjectCsvImporter#getCheckGeneratedUrisUniquenessQuery(Stream, int)
     */
    @Override
    protected SelectBuilder getCheckUrisUniquenessQuery(Stream<String> urisToCheck, int streamSize) {
        return super.getCheckUrisUniquenessQuery(urisToCheck, streamSize);
    }

    @Override
    protected void customBatchValidation(CsvOwlRestrictionValidator restrictionValidator, List<ScientificObjectModel> modelChunk, int offset) throws IOException {

        if (experiment != null) {
            try {
                scientificObjectDAO.checkUniqueNameByGraph(modelChunk, experiment);
            } catch (DuplicateNameListException e) {
                addDuplicateNameErrors(modelChunk, restrictionValidator, e.getExistingUriByName(),offset);
            } catch (SPARQLException e) {
                throw new IOException(e);
            }
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

    @Override
    public void create(CSVValidationModel validation, List<ScientificObjectModel> models) throws Exception {

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
            scientificObjectDAO.copyIntoGlobalGraph(models);
            experimentDAO.updateExperimentSpeciesFromScientificObjects(experiment);
        }

        Object geometryMetadatas = validation.getObjectsMetadata().get(Oeso.hasGeometry.getURI());

        // Geometry creation into MongoDB
        if (geometryMetadatas != null) {

            Map<SPARQLNamedResourceModel,Geometry> objectsGeometry = (Map<SPARQLNamedResourceModel,Geometry>) geometryMetadatas;

            // convert (object,geometry) map into list of GeospatialModel
             List<GeospatialModel> geospatialModels = objectsGeometry
                     .entrySet()
                     .stream()
                     .map(entry -> new GeospatialModel(entry.getKey(),graph,entry.getValue()))
                     .collect(Collectors.toList());

            geoDAO.createAll(geospatialModels);

            // clear map and values
            objectsGeometry.clear();
            geospatialModels.clear();

            validation.getObjectsMetadata().remove(Oeso.hasGeometry.getURI());
        }
    }

}
