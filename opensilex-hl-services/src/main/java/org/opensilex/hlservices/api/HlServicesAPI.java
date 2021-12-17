package org.opensilex.hlservices.api;

import org.opensilex.OpenSilexModule;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectAPI;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.extensions.APIExtension;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.inject.*;
import javax.validation.constraints.*;
import io.swagger.annotations.*;
import java.net.*;
import java.util.*;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.security.authentication.*;
import org.opensilex.server.rest.validation.*;
import org.opensilex.server.response.PaginatedListResponse;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.geojson.Geometry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


import org.opensilex.core.data.dal.DataModel;

import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.event.dal.move.PositionModel;

import org.opensilex.core.organisation.api.facitity.InfrastructureFacilityNamedDTO;
import org.opensilex.core.position.api.PositionGetDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.authentication.ApiProtected;

import org.opensilex.server.rest.validation.ValidURI;


import java.net.URI;

import java.util.List;
import org.opensilex.core.data.dal.DataDAO;

import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.utils.ListWithPagination;


@Api(HlServicesAPI.CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_ID)
@Path("/core/high_level_services")
@ApiCredentialGroup(
        groupId = HlServicesAPI.CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_ID,
        groupLabelKey = HlServicesAPI.CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_LABEL_KEY
)
public class HlServicesAPI {

    public static final String CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_ID = "High Level Services";
    public static final String CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_LABEL_KEY = "credential-groups.hl-services";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @GET
    @Path("{uri}/environmental_data")
//    @Path("/core/scientific_objects/{uri}/environmental_data")
//    @Api(ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_GROUP_ID)
    @ApiCredential(
            credentialId = HlServicesAPI.CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_ID,
            credentialLabelKey = HlServicesAPI.CREDENTIAL_HIGHLEVEL_SERVICES_GROUP_LABEL_KEY
    )
    @ApiOperation(value = "Retrieve the environmental data of a scientific object, following the move event sequence over various facilities")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "ALL NOT WELL",response = DataGetDTO.class, responseContainer = "List")
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnvironmentalData(@ApiParam(value = "scientific object URI", example = "test:set/scientific-objects/so-gui-test-4", required = true)
                                         @QueryParam("uri") @ValidURI @NotNull URI objectURI,
                                         @ApiParam(value = "Variable filter", example = "sixtine:set/variables#variable.grain_teneur_en_azote_\n")
                                         @QueryParam("variable_uri") @ValidURI List<URI> VariablesURI) throws Exception {
        // Avoir le parcours du SO
        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
        // Security check
        ScientificObjectDAO soDAO = new ScientificObjectDAO(sparql, nosql);
        ScientificObjectModel scientificObject = soDAO.getObjectByURI(objectURI, sparql.getDefaultGraphURI(ScientificObjectModel.class), null);
        MoveModel moveEvent = moveDAO.getLastMoveAfter(objectURI, null);
        LocalDate creationDate = scientificObject.getCreationDate();
        Instant destructionDate = null;
        if (Objects.nonNull(scientificObject.getDestructionDate())) {
                    destructionDate = scientificObject.getDestructionDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        ArrayList<DeviceModel> devices = new ArrayList<>();
        ArrayList<DataGetDTO> resultlist = new ArrayList<>();
        List<PositionGetDTO> resultDTOList = new ArrayList<>();
        List<DataModel> data = new ArrayList<>();
        // Helper class
        class ToAndTime {
            public final InfrastructureFacilityNamedDTO to;
            public final Instant timestamp;
            final ZoneId zone = ZoneId.of("Europe/Paris");
            public ToAndTime(InfrastructureFacilityNamedDTO _to, String _timestamp) {
                to = _to;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
                LocalDateTime dateTime = LocalDateTime.parse(_timestamp, formatter);
                ZoneOffset zoneOffSet = zone.getRules().getOffset(dateTime);
                timestamp = dateTime.toInstant(ZoneOffset.UTC);
//                timestamp = dateTime.toInstant(zoneOffSet);


            }

            ;
        }

        ArrayList<ToAndTime> SOpath_with_timestamp = new ArrayList<ToAndTime>();
        if (moveEvent != null) {

            LinkedHashMap<MoveModel, PositionModel> positionHistory = moveDAO.getPositionsHistoryWithoutPagination(
                    objectURI,
                    null,
                    null,
                    null

            );

            positionHistory.forEach((move, position) -> {
                try {
                    resultDTOList.add(new PositionGetDTO(move, position));

                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });
            // Rassemble les informations à propos des facilities
            resultDTOList.forEach((positionGetDTO) -> {
                InfrastructureFacilityNamedDTO from = positionGetDTO.getFrom();
                InfrastructureFacilityNamedDTO to = positionGetDTO.getTo();
                String moveTime = positionGetDTO.getMoveTime();
                SOpath_with_timestamp.add(new ToAndTime(to, moveTime));
            });


            DataDAO dataDAO = new DataDAO(nosql, sparql, fs);

//            ArrayList<URI> devicesURIs = new ArrayList<>();
            for (Integer index = 0; index < SOpath_with_timestamp.size(); index++) {
                ArrayList<URI> devicesURIs = (ArrayList<URI>) moveDAO.reverseSearchURIDevice(SOpath_with_timestamp.get(index).to.getUri(), MoveEventDAO.ReverseLink.TO);
                Instant start_interval = SOpath_with_timestamp.get(index).timestamp;
                Instant end_interval;
                if (index + 1 < SOpath_with_timestamp.size()) {
                    end_interval = SOpath_with_timestamp.get(index + 1).timestamp;
                } else if (destructionDate != null){
                    end_interval = destructionDate;
                } else {
                    // Proceed warning or further treatment
                    // Maybe until today with certain limits ?
                    continue;
                }
                data.addAll(dataDAO.search(currentUser,
                        null,
                        null,
                        VariablesURI,
                        null,
                        devicesURIs,
                        start_interval,
                        end_interval,
                        null,
                        null,
                        null,
                        null)
                );
            }


            data.forEach((dataModel) -> {
                resultlist.add(DataGetDTO.getDtoFromModel(dataModel));
            });


//            Not used right now
//            DeviceDAO deviceDao = new DeviceDAO(sparql, nosql);
//            devices.addAll(deviceDao.getDevicesByURI(devicesURIs, currentUser));


        }
        return new PaginatedListResponse<>(resultlist).getResponse();
    }
}