package org.opensilex.core.event.api.move.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.geojson.Point;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveEventCsvImporter extends AbstractEventCsvImporter<MoveModel> {

    public static Set<String> MOVE_SPECIFIC_HEADERS = Set.of(
            LocationModel.FROM_FIELD,
            LocationModel.TO_FIELD,
            LocationModel.GEOMETRY_FIELD,
            LocationModel.X_FIELD,
            LocationModel.Y_FIELD,
            LocationModel.Z_FIELD,
            LocationModel.TEXTUAL_POSITION_FIELD
    );

    private static final LinkedHashSet<String> MOVE_HEADER = Stream.concat(
                    AbstractEventCsvImporter.EVENT_HEADER.stream(),
                    MOVE_SPECIFIC_HEADERS.stream()
            ).collect(Collectors.toCollection(LinkedHashSet::new)
    );

    private final MoveLogic moveLogic;

    public MoveEventCsvImporter(
            SPARQLService sparql,
            OntologyDAO ontologyDAO,
            InputStream file,
            AccountModel user,
            MongoDBService mongo,
            ClientSession session
    ) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql,ontologyDAO,file, user, mongo);
        moveLogic = new MoveLogic(sparql, mongo, user, session);
    }

    @Override
    protected LinkedHashSet<String> getHeader() {
        return MOVE_HEADER;
    }

    @Override
    protected MoveModel getNewModel() {
        return new MoveModel();
    }

    @Override
    protected void readCommonsProps(MoveModel model, String[] row, int rowIndex, AtomicInteger colIndex) throws URISyntaxException, SPARQLException {

        // first call super method in order to init properties of any Event
        super.readCommonsProps(model, row, rowIndex, colIndex);

        // then fill move specific properties
        //Start by creating or getting location
        LocationModel movesLocation = MoveLogic.getOrCreateMovesLocationObservation(model, null);

        // parse from and to properties
        String from = row[colIndex.getAndIncrement()];
        boolean anyMoveFieldNonNull = moveLogic.applyValueOnFromField(from, movesLocation);

        String to = row[colIndex.getAndIncrement()];
        boolean lastValueNotNull = moveLogic.applyValueOnTooField(to, movesLocation);
        anyMoveFieldNonNull = anyMoveFieldNonNull || lastValueNotNull;

        String potentialFromTooErrorMsg = LocationLogic.validateFromAndTooValuesAndReturnErrorMsg(
                StringUtils.isEmpty(from) ? null : URI.create(from),
                StringUtils.isEmpty(to) ? null : URI.create(to)
        );
        if(potentialFromTooErrorMsg != null){
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, to,"To");
            cell.setMessage(potentialFromTooErrorMsg);
            if(potentialFromTooErrorMsg.equals(LocationLogic.FROM_BUT_NO_TOO_ERROR_MSG)){
                validation.addMissingRequiredValue(cell);
            }else {
                validation.addInvalidValueError(cell);
            }
        }

        // parse all properties which define a position : point,x,y,z,positionDescription
        String coordinates = row[colIndex.getAndIncrement()];
        try{
            lastValueNotNull = moveLogic.applyValueOnGeometryField(coordinates, movesLocation);
            anyMoveFieldNonNull = anyMoveFieldNonNull || lastValueNotNull;
        } catch (ParseException | JsonProcessingException e){
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, coordinates,"coordinates");
            validation.addInvalidValueError(cell);
        }

        String x = row[colIndex.getAndIncrement()];
        lastValueNotNull = moveLogic.applyValueOnXField(x, movesLocation);
        anyMoveFieldNonNull = anyMoveFieldNonNull || lastValueNotNull;

        String y = row[colIndex.getAndIncrement()];
        lastValueNotNull = moveLogic.applyValueOnYField(y, movesLocation);
        anyMoveFieldNonNull = anyMoveFieldNonNull || lastValueNotNull;

        String z = row[colIndex.getAndIncrement()];
        lastValueNotNull = moveLogic.applyValueOnZField(z, movesLocation);
        anyMoveFieldNonNull = anyMoveFieldNonNull || lastValueNotNull;

        String positionDescription = row[colIndex.get()];
        lastValueNotNull = moveLogic.applyValueOnTextualField(positionDescription, movesLocation);
        anyMoveFieldNonNull = anyMoveFieldNonNull || lastValueNotNull;

        if(!anyMoveFieldNonNull){
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, null,"from, to, x, y, z or positionDescription");
            validation.addMissingRequiredValue(cell);
        }
    }

}
