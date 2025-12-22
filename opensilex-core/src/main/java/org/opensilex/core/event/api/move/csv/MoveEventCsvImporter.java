package org.opensilex.core.event.api.move.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.core.event.api.csv.AbstractEventCsvImporter;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveEventCsvImporter extends AbstractEventCsvImporter<MoveModel> {

    private static final LinkedHashSet<String> MOVE_HEADER = Stream.concat(
                    AbstractEventCsvImporter.EVENT_HEADER.stream(),
                    Stream.of(
                        LocationModel.FROM_FIELD,
                        LocationModel.TO_FIELD,
                        LocationModel.GEOMETRY_FIELD,
                        LocationModel.X_FIELD,
                        LocationModel.Y_FIELD,
                        LocationModel.Z_FIELD,
                        LocationModel.TEXTUAL_POSITION_FIELD
                    )
            ).collect(Collectors.toCollection(LinkedHashSet::new)
    );

    public MoveEventCsvImporter(SPARQLService sparql, OntologyDAO ontologyDAO, InputStream file, AccountModel user) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {
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
        LocationModel locationModel = new LocationModel();


        // parse from and to properties
        String from = row[colIndex.getAndIncrement()];
        if(!StringUtils.isEmpty(from)) {
            FacilityModel fromModel = new FacilityModel();
            fromModel.setUri(new URI(from));
            locationModel.setFrom(fromModel.getUri());
            anyMoveFieldNonNull = true;
        }

        String to = row[colIndex.getAndIncrement()];
        if(!StringUtils.isEmpty(to)) {
            FacilityModel toModel = new FacilityModel();
            toModel.setUri(new URI(to));
            locationModel.setTo(toModel.getUri());
            anyMoveFieldNonNull = true;
        }

        if(StringUtils.isNotEmpty(from) && StringUtils.isEmpty(to)){
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, null,"To");
            cell.setMessage("Cannot declare a move with a 'From' value but without a 'To' value.");
            validation.addMissingRequiredValue(cell);
        }

        // parse all properties which define a position : point,x,y,z,positionDescription
        if(!CollectionUtils.isEmpty(model.getTargets())){

            String coordinates = row[colIndex.getAndIncrement()];
            if(!StringUtils.isEmpty(coordinates)){
                try{
                    locationModel.setGeometry( GeospatialDAO.wktToGeometry(coordinates));
                    anyMoveFieldNonNull = true;
                }catch (ParseException | JsonProcessingException e){
                    CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, coordinates,"coordinates");
                    validation.addInvalidValueError(cell);
                }
            }

            try{
                String x = row[colIndex.getAndIncrement()];
                if(!StringUtils.isEmpty(x)){
                    locationModel.setX(x);
                    anyMoveFieldNonNull = true;
                }

                String y = row[colIndex.getAndIncrement()];
                if(!StringUtils.isEmpty(y)){
                    locationModel.setY(y);
                    anyMoveFieldNonNull = true;
                }

                String z = row[colIndex.getAndIncrement()];
                if(!StringUtils.isEmpty(z)){
                    locationModel.setZ(z);
                    anyMoveFieldNonNull = true;
                }

            }catch (NumberFormatException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, null,"x y or z");
                validation.addMissingRequiredValue(cell);
            }

            String positionDescription = row[colIndex.get()];
            if(!StringUtils.isEmpty(positionDescription)){
                locationModel.setTextualPosition(positionDescription);
                anyMoveFieldNonNull = true;

            }
        }

        if(anyMoveFieldNonNull){
            //We don't need to set anything other than observation's location,
            // as the MoveLogic create list function handles setting of other fields
            LocationObservationModel locationObservationModel = new LocationObservationModel();
            locationObservationModel.setLocation(locationModel);
            model.setLocationObservation(locationObservationModel);
        }else {
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, null,"from, to, x, y, z or positionDescription");
            validation.addMissingRequiredValue(cell);
        }
    }

}
