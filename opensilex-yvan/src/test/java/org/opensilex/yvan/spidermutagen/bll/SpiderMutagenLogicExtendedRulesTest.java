/*
 * *****************************************************************************
 *                         SpiderMutagenLogicExtendedRulesTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 03/07/2026 09:07
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.bll;

import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.device.api.DeviceAPITest;
import org.opensilex.core.device.api.DeviceCreationDTO;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.api.GermplasmAPITest;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.api.*;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.yvan.ontology.YvanOntology;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;


public class SpiderMutagenLogicExtendedRulesTest extends AbstractMongoIntegrationTest {

    //#region static constants
    protected static final String path = "/core/scientific_objects";

    public static final String createPath = path + "/";
    public static final String updatePath = path + "/";


    public static final ServiceDescription create;
    public static final ServiceDescription update;

    static {
        try {
            create = new ServiceDescription(
                    ScientificObjectAPI.class.getMethod("createScientificObject", ScientificObjectCreationDTO.class),
                    createPath
            );
            update = new ServiceDescription(
                    ScientificObjectAPI.class.getMethod("updateScientificObject", ScientificObjectUpdateDTO.class),
                    updatePath
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    //#endregion

    //#region test setup and usefool methods

    private int soCount = 1;
    private URI experiment;
    private URI speciesUri;


    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void beforeTest() throws Exception {
        experiment = new UserCallBuilder(ExperimentAPITest.create)
                .setBody(ExperimentAPITest.getCreationDTO())
                .buildAdmin()
                .executeCallAndReturnURI();

        speciesUri = new UserCallBuilder(GermplasmAPITest.create)
                .setBody(GermplasmAPITest.getCreationSpeciesDTO())
                .buildAdmin()
                .executeCallAndReturnURI();
    }

    @After
    public void afterTest() throws Exception {
        // delete xp graph if some os has been inserted into the experiment
        SPARQLService sparql = getSparqlService();
        Node experimentNode = NodeFactory.createURI(experiment.toString());
        if (sparql.count(experimentNode, ScientificObjectModel.class) > 0) {
            sparql.clearGraph(experiment);
        }

        new UserCallBuilder(GermplasmAPITest.delete)
                .setUriInPath(speciesUri)
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.OK);

        experiment = null;
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                EventModel.class,
                LocationObservationCollectionModel.class,
                ScientificObjectModel.class,
                VariableModel.class,
                FactorModel.class,
                ExperimentModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Arrays.asList(
                LocationObservationDAO.LOCATION_COLLECTION_NAME,
                GeospatialDAO.GEOSPATIAL_COLLECTION_NAME,
                DataDAO.DATA_COLLECTION_NAME,
                DataDAO.FILE_COLLECTION_NAME,
                ProvenanceDAO.PROVENANCE_COLLECTION_NAME
        );
    }

    protected ScientificObjectCreationDTO getCreationDTO() throws Exception {
        ScientificObjectCreationDTO dto = new ScientificObjectCreationDTO();

        if (false) {
            //We need a bunch of other dtos for this
            MoveCreationDTO moveCreationDTO = new MoveCreationDTO();
            LocationObservationDTO locationObservationDTO = new LocationObservationDTO();

            List<Position> list = new LinkedList<>();
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97171243, 43.61332417));
            list.add(new Position(3.9717427, 43.61330558));
            list.add(new Position(3.97170272, 43.61327122));
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97167246, 43.61328981));

            Geometry geometry = new Polygon(list);
            Instant endInstant = Instant.now();
            locationObservationDTO.setEndDate(endInstant);
            locationObservationDTO.setGeojson(LocationLogic.geometryToGeoJson(geometry));
            moveCreationDTO.setLocation(locationObservationDTO);
            moveCreationDTO.setEnd(endInstant.toString());
            dto.setMove(moveCreationDTO);
        }

        dto.setName("SO " + soCount++);
        dto.setType(new URI("http://www.opensilex.org/vocabulary/oeso#ScientificObject"));
        if(!false){
            dto.setExperiment(experiment);
        }

        if (false) {
            RDFObjectRelationDTO germplasmRelation = new RDFObjectRelationDTO();
            germplasmRelation.setProperty(new URI(Oeso.hasGermplasm.getURI()));
            germplasmRelation.setValue(speciesUri.toString());
            List<RDFObjectRelationDTO> relationList = new ArrayList<>();
            relationList.add(germplasmRelation);
            dto.setRelations(relationList);
        } else {
            dto.setRelations(new ArrayList<>());
        }

        return dto;
    }

    private URI createDevice() throws Exception {
        var dto = new DeviceCreationDTO();
        dto.setType(URI.create(Oeso.Device.getURI()));
        dto.setName("Device");
        dto.setDescription("description");
        return new UserCallBuilder(DeviceAPITest.create)
                .setBody(dto)
                .buildAdmin()
                .executeCallAndReturnURI();
    }

    //#endregion

    @Test
    public void testCreateSpiderMutagen() throws Exception {
        URI deviceUri1 = createDevice();
        URI deviceUri2 = createDevice();

        var dto = getCreationDTO();
        dto.setType(URI.create(YvanOntology.SpiderMutagen.getURI()));
        dto.setRelations(List.of(
                new RDFObjectRelationDTO(URI.create(YvanOntology.legsNumber.getURI()), "8", false),
                new RDFObjectRelationDTO(URI.create(YvanOntology.linkedDevice.getURI()), deviceUri1.toString(), false),
                new RDFObjectRelationDTO(URI.create(YvanOntology.linkedDevice.getURI()), deviceUri2.toString(), false)
        ));

        new UserCallBuilder(SpiderMutagenLogicExtendedRulesTest.create)
                .setBody(dto)
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CONFLICT);

    }
}
