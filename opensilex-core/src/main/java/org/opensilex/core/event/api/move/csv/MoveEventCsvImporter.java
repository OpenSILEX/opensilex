package org.opensilex.core.event.api.move.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.ClientSession;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.*;
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
            ClientSession session,
            FileStorageService fs
    ) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql,ontologyDAO,file, user, mongo);
        moveLogic = new MoveLogic(sparql, mongo, user, session, fs);
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
    protected void readCommonsProps(MoveModel model, String[] row, int rowIndex, Map<Integer, String> headerByColIndex) throws Exception {
        //I don't bother calling super.readCommonProps as there are just a few lines in common at start,
        // then there are differences

        //If row size is smaller than common header size then append some InvalidValue errors and leave
        if(!verifyAndHandleNotEnoughRequiredColumnsForRow(row, rowIndex, headerByColIndex)){
            return;
        }

        boolean anyMoveFieldNonNull = false;
        LocationModel movesLocation = MoveLogic.getOrCreateMovesLocationObservation(model, null);

        //Iterate over row's common props to read and check values
        for(int i = 0; i < getHeader().size(); i++){
            String nextValue = row[i];
            String correspondingHeader = headerByColIndex.get(i);

            if(!tryToApplyValueOnGenericEventField(correspondingHeader, model, rowIndex, i, nextValue)){
                //If value didn't apply to generic event field then try Move fields
                //Make sure operation is performed before setting value of anyMoveFieldNonNull (order of stuff in Or statement was creating a bug)
                boolean nextValueWasAppliedOnSomeMoveField = applyValueOnMoveSpecificField(correspondingHeader, movesLocation, rowIndex, i, nextValue);
                anyMoveFieldNonNull = anyMoveFieldNonNull || nextValueWasAppliedOnSomeMoveField;
            }
        }
        //Now that we've finished reading row we can check which of end or start is required and relook if they were passed or not
        verifyRequiredEndOrStart(model, rowIndex, headerByColIndex);
        //Likewise check from and too
        verifyFromAndToo(movesLocation, rowIndex, headerByColIndex);

        //Add error if 0 move fields filled
        if(!anyMoveFieldNonNull){
            CSVCell cell = new CSVCell(rowIndex, getHeader().size() -1, null,"from, to, x, y, z or positionDescription");
            validation.addMissingRequiredValue(cell);
        }
    }

    /**
     * Handles the calling of LocationLogic.validateFromAndTooValuesAndReturnErrorMsg to verify From and Too values
     */
    private void verifyFromAndToo(LocationModel movesLocation, int rowIndex, Map<Integer, String> headerByColIndex) throws Exception {
        String potentialFromTooErrorMsg = LocationLogic.validateFromAndTooValuesAndReturnErrorMsg(
                movesLocation.getFrom(),
                movesLocation.getTo()
        );
        if(potentialFromTooErrorMsg != null){
            CSVCell cell = new CSVCell(rowIndex,getIndexForColumn(LocationModel.TO_FIELD, headerByColIndex), (movesLocation.getTo() == null? "No too value" : movesLocation.getTo().toString()), LocationModel.TO_FIELD);
            cell.setMessage(potentialFromTooErrorMsg);
            if(potentialFromTooErrorMsg.equals(LocationLogic.FROM_BUT_NO_TOO_ERROR_MSG)){
                validation.addMissingRequiredValue(cell);
            }else {
                validation.addInvalidValueError(cell);
            }
        }
    }

    /**
     * Tries to apply a value onto corresponding field of a LocationModel
     *
     * @param header containing the field name we are applying to
     * @param movesLocation teh LocationModel to apply on
     * @param rowIndex the current row index for error cell creating
     * @param colIndex the current column index for error cell creating
     * @param value the value we are trying to apply on field
     * @return true if a non-null value was indeed applied to a Move specific field, false otherwise
     */
    private boolean applyValueOnMoveSpecificField(String header, LocationModel movesLocation, int rowIndex, int colIndex, String value) throws URISyntaxException, SPARQLException {

        if(header.equals(LocationModel.FROM_FIELD)){
            return moveLogic.applyValueOnFromField(value, movesLocation);
        }
        if(header.equals(LocationModel.TO_FIELD)){
            return moveLogic.applyValueOnTooField(value, movesLocation);
        }
        if(header.equals(LocationModel.GEOMETRY_FIELD)){
            try{
                return moveLogic.applyValueOnGeometryField(value, movesLocation);
            } catch (ParseException | JsonProcessingException e){
                CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
                validation.addInvalidValueError(cell);
                //Return true because even if there was a parsing error, a Move field was still present
                return true;
            }
        }
        if(header.equals(LocationModel.X_FIELD)){
            return moveLogic.applyValueOnXField(value, movesLocation);
        }
        if(header.equals(LocationModel.Y_FIELD)){
            return moveLogic.applyValueOnYField(value, movesLocation);
        }
        if(header.equals(LocationModel.Z_FIELD)){
            return moveLogic.applyValueOnZField(value, movesLocation);
        }
        if(header.equals(LocationModel.TEXTUAL_POSITION_FIELD)){
            return moveLogic.applyValueOnTextualField(value, movesLocation);
        }
        return false;
    }

}
