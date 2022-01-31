package org.opensilex.core.event.api.move.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Point;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.csv.dal.CSVCell;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveEventCsvImporter extends AbstractEventCsvImporter<MoveModel> {

    private static final LinkedHashSet<String> MOVE_HEADER = Stream.concat(
                    AbstractEventCsvImporter.EVENT_HEADER.stream(),
                    Stream.of(
                        MoveModel.FROM_FIELD,
                        MoveModel.TO_FIELD,
                        PositionModel.COORDINATES_FIELD,
                        PositionModel.X_FIELD,
                        PositionModel.Y_FIELD,
                        PositionModel.Z_FIELD,
                        PositionModel.TEXTUAL_POSITION_FIELD
                    )
            ).collect(Collectors.toCollection(LinkedHashSet::new)
    );

    public MoveEventCsvImporter(SPARQLService sparql, OntologyDAO ontologyDAO, InputStream file, UserModel user) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {
        super(sparql,ontologyDAO,file, user);
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
    protected void readCommonsProps(MoveModel model, String[] row, int rowIndex, AtomicInteger colIndex) throws URISyntaxException {

        // first call super method in order to init properties of any Event
        super.readCommonsProps(model, row, rowIndex, colIndex);

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
        if(!CollectionUtils.isEmpty(model.getTargets())){

            URI targetUri = model.getTargets().get(0);
            TargetPositionModel itemPositionModel = new TargetPositionModel();
            itemPositionModel.setTarget(targetUri);

            PositionModel position = new PositionModel();
            itemPositionModel.setPosition(position);
            List<TargetPositionModel> itemPositionModels = Collections.singletonList(itemPositionModel);
            noSqlModel.setTargetPositions(itemPositionModels);

            String coordinates = row[colIndex.getAndIncrement()];
            if(!StringUtils.isEmpty(coordinates)){
                try{
                    position.setCoordinates((Point) GeospatialDAO.wktToGeometry(coordinates));
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
                position.setTextualPosition(positionDescription);
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
