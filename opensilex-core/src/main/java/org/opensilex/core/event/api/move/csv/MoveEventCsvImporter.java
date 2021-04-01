package org.opensilex.core.event.api.move.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Point;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.dal.move.ConcernedItemPositionModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MoveEventCsvImporter extends AbstractEventCsvImporter<MoveModel> {

    private final static LinkedHashSet<String> header = new LinkedHashSet<>(Arrays.asList(
            "URI", "Type", "IsInstant", "Start", "End", "Target", "Description", "From", "To", "Coordinates", "X", "Y", "Z", "TextualPosition"
    ));

    public MoveEventCsvImporter(InputStream file, URI creator){
        super(file, creator);
    }

    @Override
    protected LinkedHashSet<String> getHeader() {
        return MOVE_HEADER;
    }

    private void readAndValidateBody(CsvParser csvReader) throws IOException, URISyntaxException, ParseException {

        String[] row;

        // start at the 2nd row after two header rows
        int rowIndex = 2;

        while((row = csvReader.parseNext()) != null){
            this.models.add(readAndValidateRow(row,rowIndex));
            rowIndex++;
        }

        if(this.models.isEmpty()){
            CSVCell csvCell = new CSVCell(rowIndex, 0, null, "Empty row");
            validation.addMissingRequiredValue(csvCell);
        }
    }

    @Override
    protected void readAndValidateRow(MoveModel model, String[] row, int rowIndex, AtomicInteger colIndex) throws URISyntaxException {

        // first call super method in order to init properties of any Event
        super.readAndValidateRow(model, row, rowIndex, colIndex);

        // then fill move specific properties

        boolean anyMoveFieldNonNull = false;
        MoveEventNoSqlModel noSqlModel = new MoveEventNoSqlModel();

        // parse from and to properties
        String from = row[colIndex.getAndIncrement()];
        if(!StringUtils.isEmpty(from)) {
            InfrastructureFacilityModel fromModel = new InfrastructureFacilityModel();
            fromModel.setUri(new URI(from));
            model.setFrom(fromModel);
            anyMoveFieldNonNull = true;
        }

        String to = row[colIndex.getAndIncrement()];
        if(!StringUtils.isEmpty(to)) {
            InfrastructureFacilityModel toModel = new InfrastructureFacilityModel();
            toModel.setUri(new URI(to));
            model.setTo(toModel);
            anyMoveFieldNonNull = true;
        }

        // parse all properties which define a position : point,x,y,z,positionDescription
        if(!CollectionUtils.isEmpty(model.getConcernedItems())){

            URI concernedItemUri = model.getConcernedItems().get(0);
            ConcernedItemPositionModel itemPositionModel = new ConcernedItemPositionModel();
            itemPositionModel.setConcernedItem(concernedItemUri);

            PositionModel position = new PositionModel();
            itemPositionModel.setPosition(position);
            List<ConcernedItemPositionModel> itemPositionModels = Collections.singletonList(itemPositionModel);
            noSqlModel.setItemPositions(itemPositionModels);

            String coordinates = row[colIndex.getAndIncrement()];
            if(!StringUtils.isEmpty(coordinates)){
                try{
                    position.setPoint((Point) GeospatialDAO.wktToGeometry(coordinates));
                    anyMoveFieldNonNull = true;
                }catch (ParseException | JsonProcessingException e){
                    CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, coordinates,"coordinates");
                    validation.addInvalidValueError(cell);
                }

            }

            try{
                String x = row[colIndex.getAndIncrement()];
                if(!StringUtils.isEmpty(x)){
                    position.setX(Integer.parseInt(x));
                    anyMoveFieldNonNull = true;
                }

                String y = row[colIndex.getAndIncrement()];
                if(!StringUtils.isEmpty(y)){
                    position.setY(Integer.parseInt(y));
                    anyMoveFieldNonNull = true;
                }

                String z = row[colIndex.getAndIncrement()];
                if(!StringUtils.isEmpty(z)){
                    position.setZ(Integer.parseInt(z));
                    anyMoveFieldNonNull = true;
                }

            }catch (NumberFormatException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, null,"x y or z");
                validation.addMissingRequiredValue(cell);
            }

            String positionDescription = row[colIndex.get()];
            if(!StringUtils.isEmpty(positionDescription)){
                position.setDescription(positionDescription);
                anyMoveFieldNonNull = true;

            }
        }

        if(anyMoveFieldNonNull){
            model.setNoSqlModel(noSqlModel);
        }else {
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, null,"from, to, x, y, z or positionDescription");
            validation.addMissingRequiredValue(cell);
        }
    }

}
