//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The creations that write through the platform's own logic, on an instance in memory: events on a
 * facility, and an experiment linked to a project — with the refusals a user can meet on the way.
 *
 * @author Arnaud Charleroy
 */
public class CreationServicePlatformTest extends AbstractMongoIntegrationTest {

    private static AccountModel admin;

    private FacilityModel melgueil;
    private ProjectModel project;

    @BeforeClass
    public static void anAdministrator() {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-creation"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Before
    public void seedTheInstance() throws Exception {
        melgueil = new FacilityModel();
        melgueil.setName("Melgueil");
        getSparqlService().create(melgueil);

        project = new ProjectModel();
        project.setName("Vitis Adaptation");
        project.setStartDate(LocalDate.of(2023, 1, 1));
        getSparqlService().create(project);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(FacilityModel.class, ProjectModel.class, ExperimentModel.class, EventModel.class,
                FactorModel.class, FactorLevelModel.class, OrganizationModel.class);
    }

    private AiImportCreationService service() {
        return new AiImportCreationService(getSparqlService(), getMongoDBService(), getFs(), admin);
    }

    private AiImportSession sessionWithEvents(EventCandidate... events) {
        AiImportSession session = new AiImportSession("events", admin.getUri());
        session.setFileName("star.xlsx").setEvents(List.of(events));
        ResolutionReport report = new ResolutionReport();
        report.getFacilities().add(new ResolvedItem("Melgueil").setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(melgueil.getUri(), "Melgueil"))));
        report.getProjects().add(new ResolvedItem("Unknown project").setStatus(ResolutionStatus.MISSING));
        session.setReport(report);
        return session;
    }

    private EventCandidate spraying(String facility, int row) {
        return new EventCandidate()
                .setSheet("mngt_field")
                .setRowNumber(row)
                .setTypeLabel("Spraying")
                .setDescription("copper, 2 kg/ha")
                .setDate(LocalDate.of(2020, 5, 12))
                .setTargetKind(DataPoint.TargetKind.FACILITY)
                .addTarget(facility);
    }

    //#region events

    /**
     * An event on a known facility is written; one whose target is unknown is left out, as the
     * requirements warned it would be.
     */
    @Test
    public void theEventsOnKnownTargetsAreWritten() throws Exception {
        AiImportSession session = sessionWithEvents(spraying("Melgueil", 4), spraying("Nowhere", 5));

        CreationRequirements requirements = service().requirementsFor(CreationTarget.EVENT, session);
        assertTrue(requirements.getWarnings().toString(),
                requirements.getWarnings().contains("AiImport.proposal.warn.someEventTargetsMissing"));

        assertEquals(1, service().createEvents(session, Map.of()));
    }

    @Test
    public void eventsWithoutAnyKnownTargetAreBlocked() throws Exception {
        AiImportSession session = sessionWithEvents(spraying("Nowhere", 5));

        CreationRequirements requirements = service().requirementsFor(CreationTarget.EVENT, session);

        assertFalse(requirements.isAvailable());
        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.eventTargetsMissing"));
        assertEquals("nothing to write", 0, service().createEvents(session, Map.of()));
    }

    //#endregion

    //#region experiments

    private Map<String, String> experiment(String projects) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", "Essai ai-import");
        values.put("objective", "Tester l'assistant");
        values.put("start_date", "2020-03-01");
        values.put("end_date", "2020-10-31");
        values.put("projects", projects);
        return values;
    }

    @Test
    public void anExperimentIsCreatedWithItsProject() throws Exception {
        URI created = service().createExperiment(sessionWithEvents(), experiment(project.getUri().toString()));

        assertNotNull(created);
    }

    /**
     * The experiment runs in the facility and under the organisation the report found: the
     * platform hosts its plots only in a facility the experiment uses.
     */
    @Test
    public void anExperimentIsLinkedToItsOrganisationAndItsFacility() throws Exception {
        OrganizationModel institution = new OrganizationModel();
        institution.setName("IFV");
        new OrganizationDAO(getSparqlService()).create(institution);

        AiImportSession session = sessionWithEvents();
        session.getReport().getOrganizations().add(new ResolvedItem("IFV").setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(institution.getUri(), "IFV"))));
        CreationRequirements requirements = service().requirementsFor(CreationTarget.EXPERIMENT, session);
        String suggestedFacilities = requirements.getFields().stream()
                .filter(field -> field.getName().equals(AiImportCreationService.EXPERIMENT_FACILITIES))
                .findFirst().orElseThrow().getSuggestedValue();
        assertEquals(melgueil.getUri().toString(), suggestedFacilities);

        Map<String, String> values = experiment(project.getUri().toString());
        values.put(AiImportCreationService.EXPERIMENT_ORGANIZATIONS, institution.getUri().toString());
        values.put(AiImportCreationService.EXPERIMENT_FACILITIES, suggestedFacilities);
        URI created = service().createExperiment(session, values);

        ExperimentModel experiment = new ExperimentDAO(getSparqlService(), getMongoDBService(), getFs())
                .get(created, admin);
        assertEquals(1, experiment.getOrganizations().size());
        assertEquals(1, experiment.getFacilities().size());

        values.put(AiImportCreationService.EXPERIMENT_ORGANIZATIONS, "http://opensilex.test/id/organization/ghost");
        try {
            service().createExperiment(session, values);
            fail("no such organisation");
        } catch (CreationFieldException e) {
            assertEquals(AiImportCreationService.EXPERIMENT_ORGANIZATIONS, e.getField());
        }
    }

    /**
     * A project field that is not a URI, or names a project that does not exist, is refused on that
     * field — not deep in the storage, where nothing would say which field to correct.
     */
    @Test
    public void aWrongProjectIsRefusedOnItsField() throws Exception {
        for (String projects : List.of("not a uri at all", "http://opensilex.test/id/project/ghost")) {
            try {
                service().createExperiment(sessionWithEvents(), experiment(projects));
                fail("the project " + projects + " should be refused");
            } catch (CreationFieldException e) {
                assertEquals("projects", e.getField());
            }
        }
    }

    /**
     * Creating the experiment while the project it belongs to is still missing is allowed, and
     * warned about: the link can only be made once the project exists.
     */
    @Test
    public void anExperimentWarnsWhenItsProjectIsMissing() {
        CreationRequirements requirements = service().requirementsFor(CreationTarget.EXPERIMENT, sessionWithEvents());

        assertTrue(requirements.getWarnings().contains("AiImport.proposal.warn.projectMissing"));
    }

    //#endregion

    //#region factors

    private ExperimentModel anExperiment() throws Exception {
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName("IFV30_teisso_2024");
        experiment.setObjective("biocontrôles");
        experiment.setStartDate(LocalDate.of(2024, 1, 1));
        getSparqlService().create(experiment);
        return experiment;
    }

    private AiImportSession starSession(ExperimentModel experiment) throws Exception {
        AiImportSession session = new AiImportSession("factors", admin.getUri());
        session.setWorkbook(WorkbookFixture.starStandard()).setProfileId(StarProfile.ID);
        ResolutionReport report = new ResolutionReport();
        if (experiment != null) {
            report.getExperiments().add(new ResolvedItem(experiment.getName()).setStatus(ResolutionStatus.FOUND)
                    .setMatches(List.of(new ResourceReference(experiment.getUri(), experiment.getName()))));
        }
        session.setReport(report);
        return session;
    }

    private Map<String, String> factor(ExperimentModel experiment, String name) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(AiImportCreationService.FACTOR_EXPERIMENT, experiment.getUri().toString());
        if (name != null) {
            values.put(AiImportCreationService.FACTOR_NAME, name);
        }
        return values;
    }

    /**
     * The eleven treatments of the STAR template become the levels of one factor of the experiment,
     * named by their code — what the plots write — and described by what the file says of them.
     */
    @Test
    public void theTreatmentsBecomeTheLevelsOfAFactor() throws Exception {
        ExperimentModel experiment = anExperiment();
        AiImportSession session = starSession(experiment);

        CreationRequirements requirements = service().requirementsFor(CreationTarget.FACTORS, session);
        assertTrue(requirements.getBlockers().toString(), requirements.isAvailable());
        assertEquals(experiment.getUri().toString(), requirements.getFields().get(0).getSuggestedValue());

        assertEquals(11, service().createFactors(session, factor(experiment, "Traitement")));

        List<FactorModel> factors = new FactorDAO(getSparqlService()).getByExperiment(experiment.getUri(), "en");
        assertEquals(1, factors.size());
        FactorLevelModel copper = factors.get(0).getFactorLevels().stream()
                .filter(level -> level.getName().equals("1")).findFirst().orElseThrow();
        assertTrue(copper.getDescription(), copper.getDescription().startsWith("Cuivre tardif"));

        assertEquals("a treatment the experiment has is not created twice",
                0, service().createFactors(session, factor(experiment, "Traitement")));
    }

    @Test
    public void aFactorNeedsANameAndOneTheExperimentDoesNotHave() throws Exception {
        ExperimentModel experiment = anExperiment();
        AiImportSession session = starSession(experiment);
        FactorModel existing = new FactorModel();
        existing.setName("Traitement");
        existing.setExperiment(experiment);
        existing.setAssociatedExperiments(List.of(experiment));
        existing.setFactorLevels(List.of());
        new FactorDAO(getSparqlService()).create(existing);

        try {
            service().createFactors(session, factor(experiment, null));
            fail("the factor has no name");
        } catch (CreationFieldException e) {
            assertEquals(AiImportCreationService.FACTOR_NAME, e.getField());
        }
        try {
            service().createFactors(session, factor(experiment, "Traitement"));
            fail("the experiment already names that factor");
        } catch (CreationFieldException e) {
            assertEquals(AiImportCreationService.FACTOR_NAME, e.getField());
        }
    }

    @Test
    public void factorsWaitForTheirExperiment() throws Exception {
        CreationRequirements requirements = service().requirementsFor(CreationTarget.FACTORS, starSession(null));

        assertTrue(requirements.getWarnings().contains("AiImport.proposal.warn.experimentFirst"));
        try {
            Map<String, String> values = new LinkedHashMap<>();
            values.put(AiImportCreationService.FACTOR_EXPERIMENT, "http://opensilex.test/id/experiment/ghost");
            values.put(AiImportCreationService.FACTOR_NAME, "Traitement");
            service().createFactors(starSession(null), values);
            fail("no such experiment");
        } catch (CreationFieldException e) {
            assertEquals(AiImportCreationService.FACTOR_EXPERIMENT, e.getField());
        }
    }

    /**
     * Only a user allowed to modify factors may create them, as on the platform's factor screen.
     */
    @Test
    public void factorsNeedTheRightToModifyFactors() throws Exception {
        AccountModel guest = new AccountModel();
        guest.setUri(URI.create("test:id/account/ai-import-guest"));
        guest.setLanguage("en");
        guest.setAdmin(false);
        AiImportCreationService service = new AiImportCreationService(getSparqlService(), getMongoDBService(),
                getFs(), guest);

        CreationRequirements requirements = service.requirementsFor(CreationTarget.FACTORS, starSession(null));

        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.factorsNotAllowed"));
    }

    @Test
    public void aFileWithoutTreatmentsOffersNoFactor() {
        CreationRequirements requirements = service().requirementsFor(CreationTarget.FACTORS, sessionWithEvents());

        assertTrue(requirements.getBlockers().contains("AiImport.proposal.block.noFactorLevels"));
    }

    //#endregion
}
