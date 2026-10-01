//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.create.objects.ObjectSheetPlan;
import org.opensilex.aiimport.create.objects.TypeProperties;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Two object sheets of a workbook, two types, through the platform's real scientific-object import
 * on an instance in memory: the plain objects of one sheet, the samples of another — each sample
 * part of an object the experiment already has, and dated by a column mapped to the creation date.
 *
 * @author Arnaud Charleroy
 */
public class ObjectSheetsPlatformTest extends AbstractMongoIntegrationTest {

    private static final URI OBJECT = URI.create(Oeso.ScientificObject.getURI());
    private static final URI SAMPLE = URI.create(Oeso.ScientificObject.getNameSpace() + "Sample");

    private static AccountModel admin;

    private ExperimentModel experiment;

    @BeforeClass
    public static void anAdministrator() throws Exception {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-objects"));
        admin.setEmail(new InternetAddress("ai-import-objects@opensilex.test"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Before
    public void seedTheInstance() throws Exception {
        experiment = new ExperimentModel();
        experiment.setName("Echantillons 2024");
        experiment.setObjective("object sheets");
        experiment.setStartDate(LocalDate.of(2024, 3, 1));
        getSparqlService().create(experiment);

        ScientificObjectModel existingBlock = new ScientificObjectModel();
        existingBlock.setName("B0");
        existingBlock.setType(OBJECT);
        getSparqlService().create(SPARQLDeserializers.nodeURI(experiment.getUri()), existingBlock);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        getSparqlService().clearGraphs(experiment.getUri().toString());
    }

    private AiImportSession session(String firstSampleDate) {
        SheetStructure expe = new SheetStructure().setName("expe")
                .setHeaders(List.of("expe_id")).setRows(List.of(List.of("Echantillons 2024")));
        SheetStructure blocks = new SheetStructure().setName("ed_bloc")
                .setHeaders(List.of("bloc_id")).setRows(List.of(List.of("B1"), List.of("B2")));
        SheetStructure samples = new SheetStructure().setName("ed_sample")
                .setHeaders(List.of("sample_id", "parent_id", "sample_date", "sample_desc"))
                .setRows(List.of(List.of("S1", "B0", firstSampleDate, "first"),
                        List.of("S2", "B0", "2024-05-02", "")));
        WorkbookStructure workbook = new WorkbookStructure().setFileName("samples.xlsx")
                .setSheets(new ArrayList<>(List.of(expe, blocks, samples)));

        AiImportSession session = new AiImportSession("samples", admin.getUri());
        session.setWorkbook(workbook).setProfileId(StarProfile.ID).setReport(new ResolutionReport());
        session.getObjectPlans().put("ed_bloc", new ObjectSheetPlan("ed_bloc").setType(OBJECT));
        session.getObjectPlans().put("ed_sample", new ObjectSheetPlan("ed_sample").setType(SAMPLE)
                .map("sample_date", Oeso.hasCreationDate.getURI()));
        return session;
    }

    private ScientificObjectBulkImport importer() {
        return new ScientificObjectBulkImport(getSparqlService(), getMongoDBService(), getFs(), admin);
    }

    private Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put(ScientificObjectBulkImport.EXPERIMENT, experiment.getUri().toString());
        return values;
    }

    private Map<String, ScientificObjectModel> objectsByName() throws Exception {
        ScientificObjectSearchFilter filter = new ScientificObjectSearchFilter().setExperiment(experiment.getUri());
        filter.setLang("en").setPage(0).setPageSize(100);
        return new ScientificObjectDAO(getSparqlService()).search(filter, Collections.emptyList()).getList()
                .stream().collect(Collectors.toMap(ScientificObjectModel::getName, object -> object));
    }

    /**
     * The objects of both sheets are written together, each under its sheet's type.
     */
    @Test
    public void twoSheetsAreWrittenUnderTheirOwnTypes() throws Exception {
        BulkOutcome outcome = importer().importAll(session("2024-05-01"), values());

        assertFalse(outcome.getErrors().toString(), outcome.isRefused());
        assertEquals(4, outcome.getImported());

        Map<String, ScientificObjectModel> objects = objectsByName();
        assertEquals(objects.keySet().toString(), 5, objects.size());
        assertTrue(SPARQLDeserializers.compareURIs(OBJECT, objects.get("B1").getType()));
        assertTrue(SPARQLDeserializers.compareURIs(SAMPLE, objects.get("S1").getType()));
    }

    /**
     * A value the property's datatype refuses stops everything, and is reported on the workbook's
     * own column — the one the sample sheet calls it — not on the property's URI.
     */
    @Test
    public void aValueTheTypeRefusesIsReportedOnItsColumn() throws Exception {
        BulkOutcome outcome = importer().validate(session("tomorrow"), values());

        assertTrue(outcome.isRefused());
        RowError error = outcome.getErrors().get(0);
        assertEquals(outcome.getErrors().toString(), "ed_sample", error.getSheet());
        assertEquals(2, error.getRow());
        assertEquals("sample_date", error.getColumn());
        assertEquals("nothing was written", 1, objectsByName().size());
    }

    /**
     * What the drop-down offers comes from the platform's own ontology store: a sample has the
     * creation date and the part-of the core ontology gives every scientific object.
     */
    @Test
    public void theTypesPropertiesAreTheImportersOwn() throws Exception {
        TypeProperties properties = new TypeProperties("en");

        assertTrue(properties.isObjectType(SAMPLE));
        assertFalse(properties.isObjectType(URI.create(Oeso.Facility.getURI())));
        List<String> uris = properties.of(SAMPLE).stream().map(property -> property.uri().toString())
                .collect(Collectors.toList());
        assertTrue(uris.toString(), uris.contains(Oeso.hasCreationDate.getURI()));
        assertTrue(uris.contains(Oeso.isPartOf.getURI()));
    }
}
