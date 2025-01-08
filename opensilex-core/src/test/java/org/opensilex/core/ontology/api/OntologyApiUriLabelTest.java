/*******************************************************************************
 *                         OntologyApiTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 05/07/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.api.facility.FacilityAPI;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.api.ScientificObjectAPI;
import org.opensilex.core.scientificObject.api.ScientificObjectCreationDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.response.StatusLevel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

/**
 * This class tests the GET uris_labels service from the ontology API.
 *
 * @author rigolle
 */
public class OntologyApiUriLabelTest extends AbstractMongoIntegrationTest {

    //#region Fields

    private static final String PATH = OntologyAPI.PATH;

    private static final String URIS_LABELS_PATH = PATH + "/uris_labels";
    private static final TypeReference<SingleObjectResponse<List<NamedResourceDTO<?>>>> URIS_LABELS_RETURN_TYPE =
            new TypeReference<>() {};

    public static final ServiceDescription uriLabelService;
    static {
        try {
            uriLabelService = new ServiceDescription(
                    OntologyAPI.class.getMethod("getURILabelsList", List.class, URI.class, Boolean.class),
                    URIS_LABELS_PATH
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static SPARQLService sparql;

    private static final URI EXPERIMENT_URI = URI.create(SPARQLDeserializers.getExpandedURI("test:experimentA"));
    private static final URI FACILITY_A_URI = URI.create(SPARQLDeserializers.getExpandedURI("test:facilityA"));
    private static final URI FACILITY_B_URI = URI.create(SPARQLDeserializers.getExpandedURI("test:facilityB"));
    private static final String FACILITY_A_LABEL = "Facility A label";
    private static final String FACILITY_B_LABEL = "Facility B label";
    private static final URI SCIENTIFIC_OBJECT_A_URI = URI.create(SPARQLDeserializers.getExpandedURI("test:soA"));
    private static final URI SCIENTIFIC_OBJECT_B_URI = URI.create(SPARQLDeserializers.getExpandedURI("test:soB"));
    private static final String SCIENTIFIC_OBJECT_A_LABEL_IN_EXPERIMENT = "SO A label in experiment";
    private static final String SCIENTIFIC_OBJECT_A_LABEL_IN_GLOBAL = "SO A label in global context";
    private static final String SCIENTIFIC_OBJECT_B_LABEL = "SO B label";

    //#endregion

    //#region Setup methods

    @BeforeClass
    public static void setup() {
        sparql = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
    }

    private void createFacility(URI uri, String label) throws Exception {
        FacilityCreationDTO facility = new FacilityCreationDTO();
        facility.setUri(uri);
        facility.setName(label);
        assertEquals(Response.Status.CREATED.getStatusCode(), getJsonPostResponseAsAdmin(target(FacilityAPI.PATH), facility).getStatus());
    }

    private void createExperiment(URI uri, List<URI> facilities) throws Exception {
        ExperimentCreationDTO experiment = new ExperimentCreationDTO();
        experiment.setUri(uri);
        experiment.setName("Experiment " + uri);
        experiment.setFacilities(facilities);
        experiment.setStartDate(LocalDate.parse("2023-07-05"));
        experiment.setObjective("Objectif de " + uri);
        assertEquals(Response.Status.CREATED.getStatusCode(), getJsonPostResponseAsAdmin(target(ExperimentAPI.PATH), experiment).getStatus());
    }

    private void createScientificObject(URI uri, URI context, String name) throws Exception {
        ScientificObjectCreationDTO scientificObject = new ScientificObjectCreationDTO();
        scientificObject.setUri(uri);
        scientificObject.setName(name);
        scientificObject.setType(URI.create(Oeso.ScientificObject.getURI()));
        scientificObject.setExperiment(context);
        Response response = getJsonPostResponseAsAdmin(target(ScientificObjectAPI.PATH), scientificObject);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    //#endregion

    //#region Clean / after test methods

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                FacilityModel.class,
                ExperimentModel.class,
                ScientificObjectModel.class
        );
    }

    @Override
    public void clearGraphs() throws Exception {
        sparql.clearGraph(EXPERIMENT_URI);
        super.clearGraphs();
    }

    //#endregion

    //#region Before test methods

    @Before
    public void before() throws Exception {
        createFacility(FACILITY_A_URI, FACILITY_A_LABEL);
        createFacility(FACILITY_B_URI, FACILITY_B_LABEL);
        createExperiment(EXPERIMENT_URI, Collections.singletonList(FACILITY_A_URI));
        createScientificObject(SCIENTIFIC_OBJECT_A_URI, null, SCIENTIFIC_OBJECT_A_LABEL_IN_GLOBAL);
        createScientificObject(SCIENTIFIC_OBJECT_A_URI, EXPERIMENT_URI, SCIENTIFIC_OBJECT_A_LABEL_IN_EXPERIMENT);
        createScientificObject(SCIENTIFIC_OBJECT_B_URI, null, SCIENTIFIC_OBJECT_B_LABEL);
    }

    //#endregion

    //#region Test methods

    @Test
    public void testGetUrisLabels() throws Exception {
        var response = new UserCallBuilder(uriLabelService)
                .setBody(List.of(FACILITY_A_URI, FACILITY_B_URI, SCIENTIFIC_OBJECT_A_URI, SCIENTIFIC_OBJECT_B_URI))
                .buildAdmin()
                .executeCallAndDeserialize(URIS_LABELS_RETURN_TYPE)
                .getDeserializedResponse();

        assertEquals(0, response.getMetadata().getStatus().stream().filter(status -> status.level == StatusLevel.WARNING).count());
        assertEquals(4, response.getResult().size());
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), FACILITY_A_LABEL)));
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), FACILITY_B_LABEL)));
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), String.format("%s | %s", SCIENTIFIC_OBJECT_A_LABEL_IN_EXPERIMENT, SCIENTIFIC_OBJECT_A_LABEL_IN_GLOBAL))
                                                              || Objects.equals(dto.getName(), String.format("%s | %s", SCIENTIFIC_OBJECT_A_LABEL_IN_GLOBAL, SCIENTIFIC_OBJECT_A_LABEL_IN_EXPERIMENT))));
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), SCIENTIFIC_OBJECT_B_LABEL)));
    }

    @Test
    public void testGetUrisLabelsShouldReturnWarnings() throws Exception {
        var response = new UserCallBuilder(uriLabelService)
                .setBody(List.of(URI.create("test:unknown_uri")))
                .buildAdmin()
                .executeCallAndDeserialize(URIS_LABELS_RETURN_TYPE)
                .getDeserializedResponse();

        assertEquals(1, response.getMetadata().getStatus().stream().filter(status -> status.level == StatusLevel.WARNING).count());
    }

    @Test
    public void testGetUrisLabelsWithContext() throws Exception {
        var response = new UserCallBuilder(uriLabelService)
                .addParam("context", EXPERIMENT_URI)
                .setBody(List.of(FACILITY_A_URI, SCIENTIFIC_OBJECT_A_URI))
                .buildAdmin()
                .executeCallAndDeserialize(URIS_LABELS_RETURN_TYPE)
                .getDeserializedResponse();

        assertEquals(1, response.getMetadata().getStatus().stream().filter(status -> status.level == StatusLevel.WARNING).count());
        assertEquals(1, response.getResult().size());
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), SCIENTIFIC_OBJECT_A_LABEL_IN_EXPERIMENT)));
    }

    @Test
    public void testGetUrisLabelsWithContextAndDefault() throws Exception {
        var response = new UserCallBuilder(uriLabelService)
                .addParam("context", EXPERIMENT_URI)
                .addParam("searchDefault", true)
                .setBody(List.of(FACILITY_A_URI, SCIENTIFIC_OBJECT_A_URI))
                .buildAdmin()
                .executeCallAndDeserialize(URIS_LABELS_RETURN_TYPE)
                .getDeserializedResponse();

        assertEquals(0, response.getMetadata().getStatus().stream().filter(status -> status.level == StatusLevel.WARNING).count());
        assertEquals(2, response.getResult().size());
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), FACILITY_A_LABEL)));
        assertTrue(response.getResult().stream().anyMatch(dto -> Objects.equals(dto.getName(), SCIENTIFIC_OBJECT_A_LABEL_IN_EXPERIMENT)));
    }

    //#endregion
}
