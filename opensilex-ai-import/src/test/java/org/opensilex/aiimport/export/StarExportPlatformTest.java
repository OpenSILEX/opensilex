//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.api.AiImportAPI;
import org.opensilex.aiimport.api.StarExportAPI;
import org.opensilex.aiimport.export.ExperimentSnapshot.ExportedObject;
import org.opensilex.aiimport.export.ExperimentSnapshot.Facility;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookReader;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The export read from a real instance in memory: an experiment with its project, its
 * organisations, a located facility, a factor, two plots and their observations — then the same
 * experiment through the download endpoint, and read back as STAR.
 *
 * @author Arnaud Charleroy
 */
public class StarExportPlatformTest extends AbstractMongoIntegrationTest {

    private static AccountModel admin;

    private ExperimentModel experiment;
    private FacilityModel field;
    private VariableModel leaf;
    private PersonModel contact;

    @BeforeClass
    public static void anAdministrator() throws Exception {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-export"));
        admin.setEmail(new InternetAddress("ai-import-export@opensilex.test"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Before
    public void seedTheInstance() throws Exception {
        SPARQLService sparql = getSparqlService();

        OrganizationModel institution = new OrganizationModel();
        institution.setName("IFV");
        sparql.create(institution);
        OrganizationModel unit = new OrganizationModel();
        unit.setName("Unité de Rodilhan");
        unit.setParents(List.of(institution));
        sparql.create(unit);

        ProjectModel project = new ProjectModel();
        project.setName("CROPS-LIFE");
        project.setStartDate(LocalDate.of(2023, 1, 1));
        sparql.create(project);

        contact = new PersonModel();
        contact.setFirstName("Jeanne");
        contact.setLastName("Martin");
        contact.setEmail(new InternetAddress("jeanne.martin@opensilex.test"));
        sparql.create(contact);

        // A location is given, so the facility is not geocoded from an address over the network.
        field = new FacilityModel();
        field.setName("teissonniere");
        field.setType(URI.create(Oeso.Facility.getURI()));
        LocationObservationModel location = new LocationObservationModel();
        location.setLocation(LocationLogic.buildLocationModel(
                GeospatialDAO.wktToGeometry("POINT(4.475683 43.77175)"), null, null, null, null, null, null));
        location.setEndDate(Instant.now());
        new FacilityLogic(sparql, getMongoDBService(), admin, getFs()).create(field, List.of(location), null, admin);

        experiment = new ExperimentModel();
        experiment.setName("IFV30_teisso_2024");
        experiment.setObjective("Evaluation des biocontrôles");
        experiment.setStartDate(LocalDate.of(2024, 1, 1));
        experiment.setEndDate(LocalDate.of(2024, 12, 31));
        experiment.setProjects(List.of(project));
        experiment.setOrganizations(List.of(unit));
        experiment.setFacilities(List.of(field));
        experiment.setScientificSupervisors(List.of(contact));
        sparql.create(experiment);

        FactorModel factor = new FactorModel();
        factor.setName("Traitement");
        factor.setExperiment(experiment);
        factor.setAssociatedExperiments(List.of(experiment));
        FactorLevelModel copper = new FactorLevelModel();
        copper.setName("1");
        copper.setDescription("Cuivre tardif");
        copper.setFactor(factor);
        factor.setFactorLevels(List.of(copper));
        new FactorDAO(sparql).create(factor);

        ScientificObjectModel c1 = plot("C1", copper);
        plot("TNT3", null);

        leaf = variable();
        insert(c1.getUri(), 12.5);
        insert(field.getUri(), 1.2);
        insert(URI.create("test:id/device/station"), 3.0);
    }

    private ScientificObjectModel plot(String name, FactorLevelModel level) throws Exception {
        ScientificObjectModel plot = new ScientificObjectModel();
        plot.setName(name);
        plot.setType(URI.create(Oeso.ScientificObject.getURI()));
        if (level != null) {
            plot.setFactorLevels(List.of(level));
        }
        getSparqlService().create(SPARQLDeserializers.nodeURI(experiment.getUri()), plot);
        return plot;
    }

    private VariableModel variable() throws Exception {
        SPARQLService sparql = getSparqlService();
        EntityModel entity = new EntityModel();
        entity.setName("leaf");
        sparql.create(entity);
        CharacteristicModel characteristic = new CharacteristicModel();
        characteristic.setName("downy mildew");
        sparql.create(characteristic);
        MethodModel method = new MethodModel();
        method.setName("visual notation");
        sparql.create(method);
        UnitModel percent = new UnitModel();
        percent.setName("percent");
        percent.setSymbol("%");
        sparql.create(percent);

        VariableModel variable = new VariableModel();
        variable.setName("leaf_downy_mildew_visual_percent");
        variable.setAlternativeName("PM_LEAF_PC");
        variable.setEntity(entity);
        variable.setCharacteristic(characteristic);
        variable.setMethod(method);
        variable.setUnit(percent);
        variable.setDataType(URI.create("http://www.w3.org/2001/XMLSchema#decimal"));
        sparql.create(variable);
        return variable;
    }

    private void insert(URI target, double value) throws Exception {
        DataProvenanceModel provenance = new DataProvenanceModel();
        provenance.setUri(URI.create("test:id/provenance/star-export"));
        provenance.setExperiments(List.of(experiment.getUri()));
        DataModel data = new DataModel();
        data.setTarget(target);
        data.setVariable(leaf.getUri());
        data.setDate(Instant.parse("2024-06-01T00:00:00Z"));
        data.setOffset("Z");
        data.setIsDateTime(false);
        data.setValue(value);
        data.setProvenance(provenance);
        new DataDAO(getMongoDBService(), getSparqlService(), getFs()).create(data);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        // Not PersonModel: the persons graph also holds the administrator the test logs in as.
        return List.of(ExperimentModel.class, ProjectModel.class, OrganizationModel.class, FacilityModel.class,
                FactorModel.class, FactorLevelModel.class, VariableModel.class,
                EntityModel.class, CharacteristicModel.class, MethodModel.class, UnitModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(DataDAO.DATA_COLLECTION_NAME, LocationObservationDAO.LOCATION_COLLECTION_NAME);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        getSparqlService().clearGraphs(experiment.getUri().toString());
        getSparqlService().delete(PersonModel.class, contact.getUri());
    }

    /**
     * Everything the format asks for, read through the platform's own DAOs.
     */
    @Test
    public void theExperimentIsReadAsTheFormatNeedsIt() throws Exception {
        ExperimentSnapshot snapshot = new ExperimentSnapshotReader(getSparqlService(), getMongoDBService(), getFs(),
                admin).read(experiment.getUri());

        assertEquals("IFV30_teisso_2024", snapshot.getName());
        assertEquals(List.of("CROPS-LIFE"), snapshot.getProjects());
        assertEquals("the unit's parent is the institution", List.of("IFV"), snapshot.getOrganizations());
        assertEquals(List.of("Unité de Rodilhan"), snapshot.getSuborganizations());
        assertEquals(List.of("jeanne.martin@opensilex.test"), snapshot.getEmails());
        assertEquals(1, snapshot.getLevels().size());

        Facility facility = snapshot.getFacilities().get(0);
        assertEquals("teissonniere", facility.name());
        assertEquals(43.77175, facility.latitude(), 1e-6);
        assertEquals(4.475683, facility.longitude(), 1e-6);

        assertEquals(1, snapshot.getObjectTypes().size());
        List<ExportedObject> objects = snapshot.getObjectTypes().get(0).objects();
        assertEquals(List.of("C1", "TNT3"), objects.stream().map(ExportedObject::name).collect(Collectors.toList()));
        assertEquals(List.of("1"), objects.get(0).levels());

        assertEquals(2, snapshot.getObservations().size());
        assertEquals("the value on a device is counted, not written", 1, snapshot.getObservationsWithoutTarget());
        ExperimentSnapshot.Variable variable = snapshot.getVariables().values().iterator().next();
        assertEquals("PM_LEAF_PC", variable.code());
        assertEquals("%", variable.unit());
    }

    /**
     * The download serves the workbook, and the workbook is STAR: the profile recognises it.
     */
    @Test
    public void theWorkbookIsDownloadedAndReadsBackAsStar() throws Exception {
        Response response = appendAdminToken(target(AiImportAPI.PATH + "/" + StarExportAPI.STAR_PATH)
                .queryParam("experiment", experiment.getUri().toString())).get();

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaderString("Content-Disposition").contains("STAR_IFV30_teisso_2024.xlsx"));
        Path file = Files.createTempFile("star-download-", ".xlsx");
        try (InputStream in = response.readEntity(InputStream.class)) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            WorkbookStructure workbook = new WorkbookReader().read(file.toFile(), "STAR.xlsx");

            assertEquals(100, new StarProfile().match(workbook));
            SheetStructure plots = workbook.getSheet("ed_scientificobject").orElseThrow();
            assertEquals(List.of("C1", "TNT3"), plots.distinctValues("scientificobject_id"));
            assertTrue(workbook.getSheet("data_meteo").isPresent());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void anUnknownExperimentIsNotFound() throws Exception {
        Response response = appendAdminToken(target(AiImportAPI.PATH + "/" + StarExportAPI.STAR_PATH)
                .queryParam("experiment", "http://opensilex.test/id/experiment/ghost")).get();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void theFileNameIsSafe() {
        assertEquals("STAR_Essai_2024_nord.xlsx",
                StarExportAPI.fileName(new ExperimentSnapshot().setName("Essai 2024 / nord")));
        assertEquals("STAR_experiment.xlsx", StarExportAPI.fileName(new ExperimentSnapshot()));
    }
}
