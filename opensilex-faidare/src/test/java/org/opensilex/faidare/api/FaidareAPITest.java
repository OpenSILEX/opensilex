/*
 * *****************************************************************************
 *                         FaidareAPITest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/05/2024 23:01
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.junit.BeforeClass;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.project.api.ProjectCreationDTO;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.dal.*;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import java.util.List;
import java.util.Map;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;


public class FaidareAPITest extends AbstractMongoIntegrationTest {

    public static TestFacilityBuilder facilityBuilder;
    public static TestPersonBuilder personBuilder;
    public static TestProjectBuilder projectBuilder;
    public static TestExperimentBuilder experimentBuilder;

    public static TestVariableBuilder variableBuilder;
    public static TestEntityBuilder entityBuilder;
    public static TestMethodBuilder methodBuilder;
    public static TestUnitBuilder unitBuilder;
    public static TestCharacteristicBuilder characteristicBuilder;

    public static boolean valuesMatch(JsonNode expected, JsonNode actual, Map<String, String> keysMatching) {
        for (Map.Entry<String, String> entry : keysMatching.entrySet()) {
            if (!expected.has(entry.getKey())) {
                return false;
            } else if (!expected.get(entry.getKey()).equals(actual.get(entry.getValue())) && !SPARQLDeserializers.compareURIs(expected.get(entry.getKey()).asText(), actual.get(entry.getValue()).asText())) {
                return false;
            }
        }
        return true;
    }

    // Note : it would have been better for this to run only once before all faidare tests, but I couldn't make it work.
    @BeforeClass
    public static void faidareSetUp() throws Exception {

        OpenSilex openSilex = getOpensilex();
        SPARQLService sparql = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        MongoDBService nosql = openSilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        FileStorageService fs = openSilex.getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);
        AccountModel user = sparql.search(AccountModel.class, null).get(0);

        OrganizationDAO organizationDAO = new OrganizationDAO(sparql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);
        PersonDAO personDAO = new PersonDAO(sparql);
        ProjectDAO projectDAO = new ProjectDAO(sparql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);

        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs, user);
        BaseVariableDAO<CharacteristicModel> characteristicDAO = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
        BaseVariableDAO<MethodModel> methodDAO = new BaseVariableDAO<>(MethodModel.class, sparql);
        BaseVariableDAO<EntityModel> entityDAO = new BaseVariableDAO<>(EntityModel.class, sparql);
        BaseVariableDAO<UnitModel> unitDAO = new BaseVariableDAO<>(UnitModel.class, sparql);

        facilityBuilder = new TestFacilityBuilder();
        Geometry polygon = new Polygon(List.of(
                new Position(3.6466941625891147, 43.50868910423751),
                new Position(3.8664207250891147, 43.81071556648839),
                new Position(4.075160959464115, 43.484780209220666),
                new Position(3.6466941625891147, 43.50868910423751)
        ));
        facilityBuilder.setGeometry(geometryToGeoJson(polygon));
        for (int i=0; i<5; i++) {
            FacilityCreationDTO dto = facilityBuilder.createDTO();
            facilityDAO.create(dto.newModel(), polygon, user);
        }

        personBuilder = new TestPersonBuilder();
        for (int i=0; i<5; i++) {
            PersonDTO dto = personBuilder.createDTO();
            PersonModel model = PersonModel.fromDTO(dto, sparql);
            personDAO.create(model, null);
        }

        projectBuilder = new TestProjectBuilder();
        for (int i=0; i<5; i++) {
            projectBuilder.setCoordinators(List.of(personBuilder.getDTOList().get(i).getUri()));
            projectBuilder.setScientificContacts(List.of(personBuilder.getDTOList().get(i).getUri()));
            projectBuilder.setAdministrativeContacts(List.of(personBuilder.getDTOList().get(i).getUri()));

            ProjectCreationDTO dto = projectBuilder.createDTO();
            ProjectModel model = dto.newModel();
            projectDAO.create(model);
        }

        experimentBuilder = new TestExperimentBuilder();
        for (int i=0; i<5; i++) {
            experimentBuilder.setFacilities(List.of(facilityBuilder.getDTOList().get(i).getUri()));
            experimentBuilder.setScientificSupervisors(List.of(personBuilder.getDTOList().get(i).getUri()));
            experimentBuilder.setTechnicalSupervisors(List.of(personBuilder.getDTOList().get(i).getUri()));
            experimentBuilder.setProjects(List.of(projectBuilder.getDTOList().get(i).getUri()));

            ExperimentCreationDTO dto = experimentBuilder.createDTO();
            experimentDAO.create(dto.newModel());
        }

        variableBuilder = new TestVariableBuilder();
        entityBuilder = new TestEntityBuilder();
        methodBuilder = new TestMethodBuilder();
        unitBuilder = new TestUnitBuilder();
        characteristicBuilder = new TestCharacteristicBuilder();
        for (int i=0; i<5; i++) {
            EntityModel entityModel = entityBuilder.createDTO().newModel();
            entityDAO.create(entityModel);

            MethodModel methodModel = methodBuilder.createDTO().newModel();
            methodDAO.create(methodModel);

            UnitModel unitModel = unitBuilder.createDTO().newModel();
            unitDAO.create(unitModel);

            CharacteristicModel characteristicModel = characteristicBuilder.createDTO().newModel();
            characteristicDAO.create(characteristicModel);

            VariableModel variableModel = variableBuilder
                    .setEntity(entityModel.getUri())
                    .setMethod(methodModel.getUri())
                    .setUnit(unitModel.getUri())
                    .setCharacteristic(characteristicModel.getUri())
                    .createDTO()
                    .newModel();
            variableDAO.create(variableModel);
        }
    }
}
