package org.opensilex.core.event.api.csv;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvOwlRestrictionValidator;
import org.opensilex.sparql.csv.header.CsvHeader;
import org.opensilex.sparql.csv.validation.CsvCellValidationContext;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class EventCsvImporter extends AbstractCsvImporter<EventModel> {

    private final EventDAO<EventModel, EventSearchFilter> eventDao;

    /**
     * @param sparql     SPARQL service
     * @param mongoDB    MongoDB service (used for move and geospatial handling)
     * @param user       {@link org.opensilex.security.account.dal.AccountModel} used to determine if user has the right to access the experiment {@link ExperimentDAO#validateExperimentAccess(URI, org.opensilex.security.account.dal.AccountModel) }
     */
    public EventCsvImporter(SPARQLService sparql, MongoDBService mongoDB, AccountModel user) throws Exception {
        super(
                sparql,
                EventModel.class,
                sparql.getDefaultGraphURI(EventModel.class),
                EventModel::new,
                user.getUri()
        );
        Objects.requireNonNull(user);
        Objects.requireNonNull(mongoDB);

        eventDao = new EventDAO<>(sparql, mongoDB, EventModel.class);

    }

    /**
     *
     * Override the readRelations method to perform validations on whole row.
     * If the quantity of relations needed to validate is wrong then just don't do anything as property quantity validations have already been performed.
     * Same for Instants failing to parse, an error will have already been added for this
     */
    @Override
    protected void readRelations(int rowIdx, String[] row, CsvHeader csvHeader, EventModel model, ClassModel classModel, CsvOwlRestrictionValidator restrictionValidator) {
        super.readRelations(rowIdx, row, csvHeader, model, classModel, restrictionValidator);

        List<SPARQLModelRelation> relations = model.getRelations();
        //Set required fields and Instants, remove them from relations
        List<SPARQLModelRelation> relationsWithInstantTypesRemoved = new ArrayList<>();
        for(SPARQLModelRelation relation : relations){
            //TODO WARNING any Instant property types will have to be handled this way for subtypes of EventModel
            if(relation.getType().equals(InstantModel.class)){
                InstantModel instantModel = new InstantModel();
                instantModel.setDateTimeStamp(OffsetDateTime.parse(relation.getValue()));

                if(SPARQLDeserializers.compareURIs(relation.getProperty().toString(), Time.hasEnd.getURI())){
                    model.setEnd(instantModel);
                }else if(SPARQLDeserializers.compareURIs(relation.getProperty().toString(), Time.hasBeginning.getURI())){
                    model.setStart(instantModel);
                }
                continue;
            }
            //TODO we had to do this for concerns and isInstant because they are 'required' in the model but we are trying to use relations
            if(SPARQLDeserializers.compareURIs(relation.getProperty().toString(), Oeev.concerns.getURI())){
                try {
                    model.setTargets(Collections.singletonList(new URI(relation.getValue())));
                    continue;
                } catch (URISyntaxException e) {
                    restrictionValidator.addInvalidURIError(new CsvCellValidationContext(
                            rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                            csvHeader.getIndexes(Oeev.concerns.getURI()).get(0) + CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET,
                            "",
                            SPARQLDeserializers.getShortURI(Time.hasEnd.getURI())));
                    return;
                }
            }
            if(SPARQLDeserializers.compareURIs(relation.getProperty().toString(), Oeev.isInstant.getURI())){
                model.setIsInstant(Boolean.parseBoolean(relation.getValue()));
                continue;
            }
            relationsWithInstantTypesRemoved.add(relation);
        }
        model.setRelations(relationsWithInstantTypesRemoved);

        //Perform line validations
        //If instant then there must be an end
        //Otherwise there just needs to be a start
        //If both start and end are present then end must be after start
        if(model.getIsInstant() && model.getEnd() == null){
            restrictionValidator.addMissingRequiredValue(new CsvCellValidationContext(
                    rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                    csvHeader.getIndexes(Time.hasEnd.getURI()).get(0) + CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET,
                    "",
                    SPARQLDeserializers.getShortURI(Time.hasEnd.getURI())));
            return;
        }
        if(!model.getIsInstant()){
            if(model.getStart() == null){
                restrictionValidator.addMissingRequiredValue(new CsvCellValidationContext(
                        rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                        csvHeader.getIndexes(Time.hasBeginning.getURI()).get(0) + CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET,
                        "",
                        SPARQLDeserializers.getShortURI(Time.hasBeginning.getURI())));
                return;
            }
            if(model.getEnd() != null && model.getStart().getDateTimeStamp().isAfter(model.getEnd().getDateTimeStamp())){
                CsvCellValidationContext errorCell = new CsvCellValidationContext(
                        rowIdx + CSV_HEADER_HUMAN_READABLE_ROW_OFFSET,
                        csvHeader.getIndexes(Time.hasBeginning.getURI()).get(0) + CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET,
                        "",
                        SPARQLDeserializers.getShortURI(Time.hasBeginning.getURI()));
                errorCell.setMessage("EventCsvForm.invalidDate");
                restrictionValidator.addInvalidDateError(errorCell);
            }
        }


    }
}
