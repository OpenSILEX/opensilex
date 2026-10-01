//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.PersonCandidate;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.VariableComponent;
import org.opensilex.core.AbstractMongoIntegrationTest;
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
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The two categories resolved in more ways than by a name: variables — by ontology identifier, by
 * name or alternative name, or, when missing, component by component — and people, by the best key
 * the file gives. On an instance in memory seeded with one variable, its four components and one
 * person.
 *
 * @author Arnaud Charleroy
 */
public class VariableAndPersonResolutionTest extends AbstractMongoIntegrationTest {

    private static final String EXTERNAL_ID = "CO_356:1000217";

    private static AccountModel admin;

    private VariableModel height;
    private UnitModel centimetre;
    private MethodModel ruler;
    private PersonModel ines;

    @BeforeClass
    public static void anAdministrator() {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-variables"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Before
    public void seedTheInstance() throws Exception {
        SPARQLService sparql = getSparqlService();

        EntityModel plant = new EntityModel();
        plant.setName("plant");
        sparql.create(plant);
        CharacteristicModel characteristic = new CharacteristicModel();
        characteristic.setName("height");
        sparql.create(characteristic);
        ruler = new MethodModel();
        ruler.setName("ruler measurement");
        sparql.create(ruler);
        centimetre = new UnitModel();
        centimetre.setName("centimetre");
        centimetre.setSymbol("cm");
        sparql.create(centimetre);

        height = new VariableModel();
        height.setName("plant_height");
        height.setAlternativeName("PH");
        height.setEntity(plant);
        height.setCharacteristic(characteristic);
        height.setMethod(ruler);
        height.setUnit(centimetre);
        height.setDataType(URI.create("http://www.w3.org/2001/XMLSchema#decimal"));
        height.setExactMatch(List.of(URI.create("http://www.cropontology.org/rdf/" + EXTERNAL_ID)));
        sparql.create(height);

        ines = new PersonModel();
        ines.setFirstName("Ines");
        ines.setLastName("Chaves");
        ines.setEmail(new InternetAddress("ines.chaves@example.org"));
        sparql.create(ines);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(VariableModel.class, EntityModel.class, CharacteristicModel.class,
                MethodModel.class, UnitModel.class, PersonModel.class);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        getSparqlService().clearGraphs(CorrectionStore.graphFor(getSparqlService().getBaseURI()).toString());
    }

    private ResolutionService service() {
        return new ResolutionService(getSparqlService(), getMongoDBService(), getFs(), admin, null);
    }

    private ResolvedItem resolveVariable(VariableCandidate candidate) {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId("test");
        plan.getVariables().add(candidate);
        List<ResolvedItem> variables = service().resolve(plan).getVariables();
        assertEquals(1, variables.size());
        return variables.get(0);
    }

    private static void assertSameUri(URI expected, URI actual) {
        assertTrue(expected + " <> " + actual, SPARQLDeserializers.compareURIs(expected, actual));
    }

    private static ResolvedComponent component(ResolvedItem item, String role) {
        return item.getComponents().stream()
                .filter(component -> role.equals(component.getRole()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + role + " in " + item.getComponents()));
    }

    //#region variables

    /**
     * The ontology identifier the file carries is the most reliable key: the column's own name is
     * of no help here, and the datatype comes with the match for the type check.
     */
    @Test
    public void aVariableIsFoundByItsOntologyIdentifier() {
        ResolvedItem item = resolveVariable(new VariableCandidate("HAUTEUR").setExternalId(EXTERNAL_ID));

        assertEquals(ResolutionStatus.FOUND, item.getStatus());
        assertSameUri(height.getUri(), item.getMatches().get(0).getUri());
        assertNotNull("the datatype is fetched for the type check", item.getMatches().get(0).getDatatype());
    }

    @Test
    public void aVariableIsFoundByItsAlternativeName() {
        ResolvedItem item = resolveVariable(new VariableCandidate("PH"));

        assertEquals(ResolutionStatus.FOUND, item.getStatus());
        assertSameUri(height.getUri(), item.getMatches().get(0).getUri());
    }

    /**
     * Nowhere to be found, so its components are resolved in its place: an exact spelling is taken,
     * a near one only suggested — the variable form's selector is where the user decides.
     */
    @Test
    public void aMissingVariableHasItsComponentsResolved() {
        VariableCandidate candidate = new VariableCandidate("leaf_width")
                .setExternalId("CO_999:0000001")
                .setTrait(new VariableComponent("height", null))
                .setMethod(new VariableComponent("rulr measurement", null))
                .setUnit(new VariableComponent("centimetre", "UO:0000015"));

        ResolvedItem item = resolveVariable(candidate);

        assertEquals(ResolutionStatus.MISSING, item.getStatus());
        assertEquals("AiImport.report.hint.variableMissingWithExternalId", item.getHintMessage().getKey());
        assertSameUri(centimetre.getUri(), component(item, "unit").getUri());
        ResolvedComponent method = component(item, "method");
        assertNull("a misspelt method is suggested, not taken", method.getUri());
        assertNotNull(method.getSuggestion());
        assertSameUri(ruler.getUri(), method.getSuggestion().getUri());
    }

    @Test
    public void aConfirmedVariableComesWithItsDatatype() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.VARIABLES, "hauteur_plante",
                new ResourceReference(height.getUri(), height.getName()));
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId("test");
        plan.getVariables().add(new VariableCandidate("hauteur_plante"));

        ResolvedItem item = service().resolve(plan, confirmed).getVariables().get(0);

        assertEquals(ResolutionStatus.FOUND, item.getStatus());
        assertNotNull(item.getMatches().get(0).getDatatype());
    }

    @Test
    public void aMisspeltVariableIsSuggested() {
        ResolvedItem item = resolveVariable(new VariableCandidate("plant_heigth"));

        assertEquals(ResolutionStatus.MISSING, item.getStatus());
        assertFalse(item.getSuggestions().isEmpty());
        assertSameUri(height.getUri(), item.getSuggestions().get(0).getUri());
    }

    @Test
    public void aTaughtVariableIsReadBackUnderTheUsersRights() throws Exception {
        new CorrectionStore(getSparqlService(), CorrectionStore.graphFor(getSparqlService().getBaseURI()))
                .remember(ReportCategory.VARIABLES, "haut_pl", height.getUri(), height.getName(),
                        admin.getUri(), "Admin");

        ResolvedItem item = resolveVariable(new VariableCandidate("haut_pl"));

        assertEquals(ResolutionStatus.FOUND, item.getStatus());
        assertEquals("AiImport.report.hint.learnedCorrection", item.getHintMessage().getKey());
    }

    //#endregion

    //#region people

    @Test
    public void aPersonIsFoundByTheirEmail() {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId("test");
        plan.getPersons().add(new PersonCandidate("I. Chaves").setEmail("ines.chaves@example.org"));

        ResolvedItem item = service().resolve(plan).getPersons().get(0);

        assertEquals(ResolutionStatus.FOUND, item.getStatus());
        assertSameUri(ines.getUri(), item.getMatches().get(0).getUri());
    }

    @Test
    public void aMisspeltPersonIsSuggestedAndVisibleByUri() {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId("test");
        plan.getPersons().add(new PersonCandidate("Ines Chavez"));

        ResolvedItem item = service().resolve(plan).getPersons().get(0);

        assertEquals(ResolutionStatus.MISSING, item.getStatus());
        assertFalse(item.getSuggestions().isEmpty());
        Optional<ResourceReference> visible = service().visibleResource(ReportCategory.PERSONS, ines.getUri());
        assertEquals("Ines Chaves", visible.orElseThrow(AssertionError::new).getName());
    }

    //#endregion
}
