package org.opensilex.core.event.api.move.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Point;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.core.position.dal.ConcernedItemPositionModel;
import org.opensilex.core.position.dal.PositionNoSqlModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class MoveEventCsvImporter {

    private final InputStream file;
    private final CSVValidationModel validation;
    private final List<MoveModel> models;
    private final URI creator;

    private final static LinkedHashSet<String> header = new LinkedHashSet<>(Arrays.asList(
        "URI", "Type", "IsInstant", "Start", "End", "Target", "Description", "From", "To", "Coordinates", "X", "Y", "Z", "TextualPosition"
    ));

    public MoveEventCsvImporter(InputStream file, URI creator) throws ParseException, IOException, URISyntaxException {
        this.file = file;
        this.creator = creator;
        models = new ArrayList<>();
        validation = new CSVValidationModel();
        readFile();
    }

    public List<MoveModel> getModels() {
        return models;
    }

    public CSVValidationModel getValidation() {
        return validation;
    }

    private void readFile() throws ParseException, IOException, URISyntaxException {

        try(CSVReader csvReader = new CSVReader(new InputStreamReader(file))) {

            readAndValidateHeader(csvReader);

            if(! validation.hasErrors()){
                readAndValidateBody(csvReader);
            }

        }catch (IOException | URISyntaxException | ParseException e){
            throw e;
        }

    }

    private void readAndValidateHeader(CSVReader csvReader) throws IOException {

        String[] csvFileHeader = csvReader.readNext();
        List<String> missingHeaders = new LinkedList<>();

        if(csvFileHeader == null || csvFileHeader.length == 0){
            throw new IllegalArgumentException("Empty csv header");
        }

        // ensure header is equals to the expected header
        Iterator<String> headerIt = header.iterator();
        int i = 0;

        while (headerIt.hasNext()){
            String expectedHeader = headerIt.next();

            if(i < csvFileHeader.length){
                String fileHeader = csvFileHeader[i];
                if(! expectedHeader.equals(fileHeader)){
                    validation.addInvalidHeaderURI(i,fileHeader);
                }
                i++;
            }
            else{
                missingHeaders.add(expectedHeader);
            }
        }

        if(! missingHeaders.isEmpty()){
            validation.addMissingHeaders(missingHeaders);
        }

        // skip the header description row
        csvReader.readNext();
    }

    private void readAndValidateBody(CSVReader csvReader) throws IOException, URISyntaxException, ParseException {

        String[] row;

        // start at the 2nd row after two header rows
        int rowIndex = 2;

        while((row = csvReader.readNext()) != null){
            this.models.add(readAndValidateRow(row,rowIndex));
            rowIndex++;
        }

        if(this.models.isEmpty()){
            CSVCell csvCell = new CSVCell(rowIndex, 0, null, "Empty row");
            validation.addMissingRequiredValue(csvCell);
        }
    }

    private MoveModel readAndValidateRow(String[] row, int rowIndex) throws URISyntaxException {

        MoveModel sparqlModel = new MoveModel();

        int colIndex = 0;
        String uri = row[colIndex++];
        if(!StringUtils.isEmpty(uri)){
            sparqlModel.setUri(new URI(uri));
        }

        String type = row[colIndex++];
        if(!StringUtils.isEmpty(type)){
            sparqlModel.setType(new URI(type));
        }

        String isInstant = row[colIndex++];
        if (StringUtils.isEmpty(isInstant)) {
            CSVCell cell = new CSVCell(rowIndex,colIndex-1, isInstant,"isInstant");
            validation.addMissingRequiredValue(cell);
        }else{
            sparqlModel.setIsInstant(Boolean.parseBoolean(isInstant));
        }

        String start = row[colIndex++];
        if(! StringUtils.isEmpty(start)) {
            InstantModel startModel = new InstantModel();
            try{
                startModel.setDateTimeStamp(OffsetDateTime.parse(start));
                sparqlModel.setStart(startModel);
            }catch (DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex-1, start,"start");
                validation.addInvalidValueError(cell);
            }
        }

        String end = row[colIndex++];
        if (StringUtils.isEmpty(end)) {

            if(sparqlModel.getIsInstant()){
                CSVCell cell = new CSVCell(rowIndex,colIndex-1, end,"end");
                validation.addMissingRequiredValue(cell);
            }
            else if (StringUtils.isEmpty(start)) {
                CSVCell cell = new CSVCell(rowIndex,colIndex-2, start,"start");
                validation.addMissingRequiredValue(cell);
            }

        }else{
            InstantModel endModel = new InstantModel();
            try {
                endModel.setDateTimeStamp(OffsetDateTime.parse(end));
                sparqlModel.setEnd(endModel);
            }catch (DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex-1, end,"end");
                validation.addInvalidValueError(cell);
            }

        }

        String concernedItem = row[colIndex++];
        URI concernedItemUri = null;

        if(StringUtils.isEmpty(concernedItem )){
            CSVCell cell = new CSVCell(rowIndex,colIndex-1, concernedItem,"Target");
            validation.addMissingRequiredValue(cell);
        }else{
            concernedItemUri = new URI(concernedItem);
            sparqlModel.setConcernedItems(Collections.singletonList(concernedItemUri));
        }

        String description = row[colIndex++];
        sparqlModel.setDescription(description);
        sparqlModel.setCreator(creator);


        boolean anyMoveFieldNonNull = false;
        MoveEventNoSqlModel noSqlModel = new MoveEventNoSqlModel();

        String from = row[colIndex++];
        if(!StringUtils.isEmpty(from)) {
            InfrastructureFacilityModel fromModel = new InfrastructureFacilityModel();
            fromModel.setUri(new URI(from));
            sparqlModel.setFrom(fromModel);
            anyMoveFieldNonNull = true;
        }

        String to = row[colIndex++];
        if(!StringUtils.isEmpty(to)) {
            InfrastructureFacilityModel toModel = new InfrastructureFacilityModel();
            toModel.setUri(new URI(to));
            sparqlModel.setTo(toModel);
            anyMoveFieldNonNull = true;
        }

        ConcernedItemPositionModel itemPositionModel = new ConcernedItemPositionModel();

        if(concernedItem != null){

            itemPositionModel.setConcernedItem(concernedItemUri);

            PositionNoSqlModel position = new PositionNoSqlModel();
            itemPositionModel.setPosition(position);
            List<ConcernedItemPositionModel> itemPositionModels = Collections.singletonList(itemPositionModel);
            noSqlModel.setItemPositions(itemPositionModels);

            String coordinates = row[colIndex++];
            if(!StringUtils.isEmpty(coordinates)){
                try{
                    position.setPoint((Point) GeospatialDAO.wktToGeometry(coordinates));
                    anyMoveFieldNonNull = true;
                }catch (ParseException | JsonProcessingException e){
                    CSVCell cell = new CSVCell(rowIndex,colIndex-1, coordinates,"coordinates");
                    validation.addInvalidValueError(cell);
                }

            }

            try{
                String x = row[colIndex++];
                if(!StringUtils.isEmpty(x)){
                    position.setX(Integer.parseInt(x));
                    anyMoveFieldNonNull = true;
                }

                String y = row[colIndex++];
                if(!StringUtils.isEmpty(y)){
                    position.setY(Integer.parseInt(y));
                    anyMoveFieldNonNull = true;
                }

                String z = row[colIndex++];
                if(!StringUtils.isEmpty(z)){
                    position.setZ(Integer.parseInt(z));
                    anyMoveFieldNonNull = true;
                }

            }catch (NumberFormatException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex-1, null,"x y or z");
                validation.addMissingRequiredValue(cell);
            }

            String positionDescription = row[colIndex];
            if(!StringUtils.isEmpty(positionDescription)){
                position.setDescription(positionDescription);
                anyMoveFieldNonNull = true;

            }
        }

        if(anyMoveFieldNonNull){
            sparqlModel.setNoSqlModel(noSqlModel);
        }else {
            CSVCell cell = new CSVCell(rowIndex,colIndex-1, null,"from, to, x, y, z or positionDescription");
            validation.addMissingRequiredValue(cell);
        }

        return sparqlModel;
    }

}
