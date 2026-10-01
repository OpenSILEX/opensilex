//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The whole resolution against a real instance — RDF4J in memory, an embedded MongoDB — seeded with
 * one resource of each kind. It is the net under {@link ResolutionService}: every order of
 * confidence (exact, confirmed, learned, near) is exercised through the public entry point only.
 *
 * @author Arnaud Charleroy
 */
public class ResolutionServiceTest extends AbstractMongoIntegrationTest {

    private static AccountModel admin;

    private ExperimentModel experiment;
    private ProjectModel project;
    private GermplasmModel pinotGris;
    private FacilityModel field;

    @BeforeClass
    public static void anAdministrator() {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-admin"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Before
    public void seedTheInstance() throws Exception {
        SPARQLService sparql = getSparqlService();

        experiment = new ExperimentModel();
        experiment.setName("Vigne Nord 2024");
        experiment.setObjective("resolution");
        experiment.setStartDate(LocalDate.of(2024, 3, 1));
        sparql.create(experiment);

        project = new ProjectModel();
        project.setName("Vitis Adaptation");
        project.setShortname("VITADAPT");
        project.setStartDate(LocalDate.of(2023, 1, 1));
        sparql.create(project);

        pinotGris = new GermplasmModel();
        // The label needs its language, or the store keeps no rdfs:label at all.
        pinotGris.setLabel(new SPARQLLabel("Pinot gris", "en"));
        pinotGris.setType(URI.create(Oeso.Species.getURI()));
        pinotGris.setSynonyms(List.of("Pinot Grigio"));
        sparql.create(pinotGris);

        field = new FacilityModel();
        field.setName("Parcelle Nord");
        sparql.create(field);

        ScientificObjectModel plot = new ScientificObjectModel();
        plot.setName("P-001");
        plot.setType(URI.create(Oeso.ScientificObject.getURI()));
        sparql.create(SPARQLDeserializers.nodeURI(experiment.getUri()), plot);
    }

    /**
     * The plots live in the experiment's own graph and the corrections in the module's: neither is
     * a default graph, so neither is cleared by {@link #getModelsToClean()}.
     */
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        getSparqlService().clearGraphs(experiment.getUri().toString(),
                CorrectionStore.graphFor(getSparqlService().getBaseURI()).toString());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, ProjectModel.class, GermplasmModel.class,
                FacilityModel.class, ScientificObjectModel.class, OrganizationModel.class);
    }

    private ResolutionService service() {
        return new ResolutionService(getSparqlService(), getMongoDBService(), getFs(), admin, null);
    }

    private ExtractedImportPlan plan() {
        return new ExtractedImportPlan().setProfileId("test");
    }

    private ResolvedItem only(List<ResolvedItem> items) {
        assertEquals(items.toString(), 1, items.size());
        return items.get(0);
    }

    private String hintKey(ResolvedItem item) {
        return item.getHintMessage() == null ? null : item.getHintMessage().getKey();
    }

    private void assertFound(ResolvedItem item, URI uri) {
        assertEquals(item.getSourceValue() + ": " + hintKey(item), ResolutionStatus.FOUND, item.getStatus());
        assertSameUri(uri, item.getMatches().get(0).getUri());
    }

    /**
     * The store hands URIs back in their short form ("test:id/…") when the model kept the long one.
     */
    private void assertSameUri(URI expected, URI actual) {
        assertTrue(expected + " <> " + actual, SPARQLDeserializers.compareURIs(expected, actual));
    }

    //#region exact

    @Test
    public void everyKindIsFoundWhenSpeltAsInTheInstance() {
        ExtractedImportPlan plan = plan();
        plan.getExperimentNames().add("vigne nord 2024");
        plan.getProjectNames().add("VITADAPT");
        plan.getGermplasmNames().add("Pinot gris");
        plan.getFacilityNames().add("Parcelle Nord");
        plan.getScientificObjectNames().add("P-001");

        ResolutionReport report = service().resolve(plan);

        assertFound(only(report.getExperiments()), experiment.getUri());
        assertFound(only(report.getProjects()), project.getUri());
        assertFound(only(report.getGermplasm()), pinotGris.getUri());
        assertFound(only(report.getFacilities()), field.getUri());
        assertEquals(ResolutionStatus.FOUND, only(report.getScientificObjects()).getStatus());
        assertTrue(report.getWarnings().toString(), report.getWarnings().isEmpty());
    }

    @Test
    public void aVarietyWrittenUnderItsSynonymIsFound() {
        ExtractedImportPlan plan = plan();
        plan.getGermplasmNames().add("Pinot Grigio");

        assertFound(only(service().resolve(plan).getGermplasm()), pinotGris.getUri());
    }

    @Test
    public void anUnknownNameIsMissingWithItsHint() {
        ExtractedImportPlan plan = plan();
        plan.getExperimentNames().add("Blé Sud 2019");
        plan.getProjectNames().add("Unknown project");

        ResolutionReport report = service().resolve(plan);

        ResolvedItem experimentItem = only(report.getExperiments());
        assertEquals(ResolutionStatus.MISSING, experimentItem.getStatus());
        assertEquals("AiImport.report.hint.experimentMissing", hintKey(experimentItem));
        assertEquals(ResolutionStatus.MISSING, only(report.getProjects()).getStatus());
    }

    /**
     * Plots live inside an experiment: without one, nothing is looked up and the report says why.
     */
    @Test
    public void objectsWaitForTheirExperiment() {
        ExtractedImportPlan plan = plan();
        plan.getScientificObjectNames().add("P-001");

        ResolvedItem item = only(service().resolve(plan).getScientificObjects());
        assertEquals(ResolutionStatus.NOT_CHECKED, item.getStatus());
        assertEquals("AiImport.report.hint.objectsNeedAnExperiment", hintKey(item));
    }

    //#endregion

    //#region confirmed, learned, near

    @Test
    public void aMisspeltFacilityIsSuggestedNotResolved() {
        ExtractedImportPlan plan = plan();
        plan.getFacilityNames().add("Parcele Nord");

        ResolvedItem item = only(service().resolve(plan).getFacilities());
        assertEquals(ResolutionStatus.MISSING, item.getStatus());
        assertEquals("AiImport.report.hint.didYouMean", hintKey(item));
        assertSameUri(field.getUri(), item.getSuggestions().get(0).getUri());
    }

    @Test
    public void aMisspeltVarietyIsSuggestedFromItsFragments() {
        ExtractedImportPlan plan = plan();
        plan.getGermplasmNames().add("Pinot griss");

        ResolvedItem item = only(service().resolve(plan).getGermplasm());
        assertEquals(ResolutionStatus.MISSING, item.getStatus());
        assertFalse(item.getSuggestions().isEmpty());
        assertSameUri(pinotGris.getUri(), item.getSuggestions().get(0).getUri());
    }

    @Test
    public void aConfirmedMatchResolvesLikeACorrectSpelling() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.PROJECTS, "Vitis Adapt.",
                new ResourceReference(project.getUri(), project.getName()));
        ExtractedImportPlan plan = plan();
        plan.getProjectNames().add("Vitis Adapt.");

        assertFound(only(service().resolve(plan, confirmed).getProjects()), project.getUri());
    }

    @Test
    public void aConfirmedExperimentStillLetsItsPlotsBeFound() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.EXPERIMENTS, "VN24",
                new ResourceReference(experiment.getUri(), experiment.getName()));
        ExtractedImportPlan plan = plan();
        plan.getExperimentNames().add("VN24");
        plan.getScientificObjectNames().add("P-001");

        ResolutionReport report = service().resolve(plan, confirmed);
        assertFound(only(report.getExperiments()), experiment.getUri());
        assertEquals(ResolutionStatus.FOUND, only(report.getScientificObjects()).getStatus());
    }

    @Test
    public void aTaughtCorrectionResolvesOutrightAndSaysSo() throws Exception {
        store().remember(ReportCategory.FACILITIES, "Pcl Nord", field.getUri(), field.getName(),
                admin.getUri(), "Admin");
        ExtractedImportPlan plan = plan();
        plan.getFacilityNames().add("Pcl Nord");

        ResolvedItem item = only(service().resolve(plan).getFacilities());
        assertFound(item, field.getUri());
        assertEquals("AiImport.report.hint.learnedCorrection", hintKey(item));
    }

    @Test
    public void aTaughtExperimentAlsoOpensItsPlots() throws Exception {
        store().remember(ReportCategory.EXPERIMENTS, "Vigne N. 24", experiment.getUri(),
                experiment.getName(), admin.getUri(), "Admin");
        ExtractedImportPlan plan = plan();
        plan.getExperimentNames().add("Vigne N. 24");
        plan.getScientificObjectNames().add("P-001");

        ResolutionReport report = service().resolve(plan);
        assertFound(only(report.getExperiments()), experiment.getUri());
        assertEquals(ResolutionStatus.FOUND, only(report.getScientificObjects()).getStatus());
    }

    /**
     * A platform form may name what it creates differently from the file — the variable form names
     * a variable after its components. The row is then bound by URI, read back under the user's
     * rights, and resolves whatever the resource is called.
     */
    @Test
    public void aResourceCreatedForARowIsBoundByItsUriWhateverItsName() {
        ResourceReference created = service().visibleResource(ReportCategory.FACILITIES, field.getUri())
                .orElseThrow(AssertionError::new);
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.FACILITIES, "Champ du haut", created);
        ExtractedImportPlan plan = plan();
        plan.getFacilityNames().add("Champ du haut");

        assertFound(only(service().resolve(plan, confirmed).getFacilities()), field.getUri());
    }

    @Test
    public void aUriNobodyCreatedIsNotBound() {
        assertFalse(service().visibleResource(ReportCategory.FACILITIES,
                URI.create("test:id/organization/facility.nothing")).isPresent());
    }

    private CorrectionStore store() {
        return new CorrectionStore(getSparqlService(),
                CorrectionStore.graphFor(getSparqlService().getBaseURI()));
    }

    //#endregion

    //#region organisations and the field's details

    /**
     * The institution the instance has is found; its unit, missing, keeps the institution the
     * file places it in, for the form that creates it to start from.
     */
    @Test
    public void anOrganisationIsFoundAndAUnitKeepsItsInstitution() throws Exception {
        OrganizationModel institution = new OrganizationModel();
        institution.setName("IFV");
        // Through the DAO, as the platform creates one: it clears the organisations it caches per user.
        new OrganizationDAO(getSparqlService()).create(institution);

        ExtractedImportPlan plan = plan();
        plan.getOrganizationNames().addAll(List.of("IFV", "Unité de Rodilhan"));
        plan.getOrganizationParents().put("Unité de Rodilhan", "IFV");

        List<ResolvedItem> items = service().resolve(plan).getOrganizations();

        assertEquals(2, items.size());
        assertFound(items.get(0), institution.getUri());
        assertEquals(ResolutionStatus.MISSING, items.get(1).getStatus());
        assertEquals("IFV", items.get(1).getParentValue());
        assertEquals("AiImport.report.hint.organizationMissing", hintKey(items.get(1)));
    }

    /**
     * A misspelt organisation is suggested, never resolved.
     */
    @Test
    public void aMisspeltOrganisationIsSuggested() throws Exception {
        OrganizationModel institution = new OrganizationModel();
        institution.setName("Institut de la Vigne");
        new OrganizationDAO(getSparqlService()).create(institution);
        ExtractedImportPlan plan = plan();
        plan.getOrganizationNames().add("Institut de la Vignee");

        ResolvedItem item = only(service().resolve(plan).getOrganizations());

        assertEquals(ResolutionStatus.MISSING, item.getStatus());
        assertFalse(item.getSuggestions().isEmpty());
    }

    /**
     * What the file says of a field travels on its row, for the facility form.
     */
    @Test
    public void aFieldKeepsWhatTheFileSaysOfIt() {
        ExtractedImportPlan plan = plan();
        plan.getFacilityNames().add("Parcelle Sud");
        plan.getFacilityDetails().put("Parcelle Sud", java.util.Map.of("town", "Bellegarde"));

        ResolvedItem item = only(service().resolve(plan).getFacilities());

        assertEquals("Bellegarde", item.getDetails().get("town"));
    }

    //#endregion
}
