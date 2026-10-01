//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryDao;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The observations of a workbook, through the platform's real data import: its validation, its
 * insertion, its batch history — on an instance in memory, seeded with an experiment, three plots
 * and a decimal variable.
 *
 * @author Arnaud Charleroy
 */
public class DataBulkImportPlatformTest extends AbstractMongoIntegrationTest {

    private static final String HEIGHT = "height";
    private static final String SHEET = "data";

    private static AccountModel admin;

    private ExperimentModel experiment;
    private VariableModel height;
    private final List<ScientificObjectModel> plots = new ArrayList<>();

    @BeforeClass
    public static void anAdministrator() throws Exception {
        admin = new AccountModel();
        admin.setUri(URI.create("test:id/account/ai-import-data"));
        // The data import names its temporary files after the account's email.
        admin.setEmail(new InternetAddress("ai-import@opensilex.test"));
        admin.setLanguage("en");
        admin.setAdmin(true);
    }

    @Before
    public void seedTheInstance() throws Exception {
        SPARQLService sparql = getSparqlService();

        experiment = new ExperimentModel();
        experiment.setName("Vigne Nord 2024");
        experiment.setObjective("data import");
        experiment.setStartDate(LocalDate.of(2024, 3, 1));
        sparql.create(experiment);

        plots.clear();
        for (String name : List.of("P-001", "P-002", "P-003")) {
            ScientificObjectModel plot = new ScientificObjectModel();
            plot.setName(name);
            plot.setType(URI.create(Oeso.ScientificObject.getURI()));
            sparql.create(SPARQLDeserializers.nodeURI(experiment.getUri()), plot);
            plots.add(plot);
        }

        EntityModel entity = new EntityModel();
        entity.setName("plant");
        sparql.create(entity);
        CharacteristicModel characteristic = new CharacteristicModel();
        characteristic.setName("height");
        sparql.create(characteristic);
        MethodModel method = new MethodModel();
        method.setName("ruler");
        sparql.create(method);
        UnitModel unit = new UnitModel();
        unit.setName("centimetre");
        unit.setSymbol("cm");
        sparql.create(unit);

        height = new VariableModel();
        height.setName("plant_height_ruler_cm");
        height.setEntity(entity);
        height.setCharacteristic(characteristic);
        height.setMethod(method);
        height.setUnit(unit);
        height.setDataType(URI.create("http://www.w3.org/2001/XMLSchema#decimal"));
        sparql.create(height);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return List.of(ExperimentModel.class, VariableModel.class, EntityModel.class,
                CharacteristicModel.class, MethodModel.class, UnitModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(DataDAO.DATA_COLLECTION_NAME, ProvenanceDAO.PROVENANCE_COLLECTION_NAME,
                BatchHistoryDao.BATCH_HISTORY_COLLECTION_NAME);
    }

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        getSparqlService().clearGraphs(experiment.getUri().toString());
    }

    //#region import

    @Test
    public void theObservationsAreWrittenWithTheirBatchHistory() throws Exception {
        AiImportSession session = session(point(0, 2, "12.5"), point(1, 3, "14"));

        BulkOutcome outcome = importer(Integer.MAX_VALUE).importAll(session, values());

        assertFalse(outcome.getErrors().toString(), outcome.isRefused());
        assertEquals(2, outcome.getImported());
        assertEquals(1, outcome.getBatches().size());
        assertEquals(2, count(DataDAO.DATA_COLLECTION_NAME));
        assertEquals("the provenance of the run is kept", 1,
                count(ProvenanceDAO.PROVENANCE_COLLECTION_NAME));
    }

    /**
     * A value the variable's type refuses is reported on the user's row and column, and nothing is
     * written — not the valid rows, not the provenance made for the run.
     */
    @Test
    public void aValueOfTheWrongTypeIsRefusedOnItsRowAndNothingIsWritten() throws Exception {
        AiImportSession session = session(point(0, 2, "12.5"), point(1, 3, "tall"));

        BulkOutcome outcome = importer(Integer.MAX_VALUE).importAll(session, values());

        assertTrue(outcome.isRefused());
        RowError error = outcome.getErrors().get(0);
        assertEquals(outcome.getErrors().toString(), RowError.Kind.INVALID_DATATYPE, error.getKind());
        assertEquals(SHEET, error.getSheet());
        assertEquals(3, error.getRow());
        assertEquals("the workbook's column, not the variable's URI", HEIGHT, error.getColumn());
        assertEquals(0, count(DataDAO.DATA_COLLECTION_NAME));
        assertEquals("the provenance made for the run is gone", 0,
                count(ProvenanceDAO.PROVENANCE_COLLECTION_NAME));
    }

    /**
     * Larger than one platform import: every batch validated, then every batch written, each with
     * its own batch history.
     */
    @Test
    public void aLargeFileGoesInBatches() throws Exception {
        AiImportSession session = session(point(0, 2, "12.5"), point(1, 3, "14"), point(2, 4, "9.8"));

        BulkOutcome outcome = importer(1).importAll(session, values());

        assertFalse(outcome.getErrors().toString(), outcome.isRefused());
        assertEquals(3, outcome.getImported());
        assertEquals(3, outcome.getBatches().size());
        assertEquals(3, count(DataDAO.DATA_COLLECTION_NAME));
    }

    /**
     * The last batch is wrong: every batch is validated before the first is written, so nothing is —
     * and the error lands on the last batch's workbook row, not on row 0 of its batch.
     */
    @Test
    public void anErrorInTheLastBatchStopsEveryBatch() throws Exception {
        AiImportSession session = session(point(0, 2, "12.5"), point(1, 3, "14"), point(2, 4, "tall"));

        BulkOutcome outcome = importer(1).importAll(session, values());

        assertTrue(outcome.isRefused());
        assertEquals(4, outcome.getErrors().get(0).getRow());
        assertEquals(0, outcome.getImported());
        assertEquals(0, count(DataDAO.DATA_COLLECTION_NAME));
    }

    //#endregion

    //#region fixtures

    private DataBulkImport importer(int batchSize) {
        return new DataBulkImport(getSparqlService(), getMongoDBService(), getFs(), admin, batchSize);
    }

    private AiImportSession session(DataPoint... points) {
        AiImportSession session = new AiImportSession("data-test", admin.getUri());
        session.setFileName("data.xlsx").setDataPoints(List.of(points));

        ResolutionReport report = new ResolutionReport();
        report.getExperiments().add(new ResolvedItem(experiment.getName())
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(experiment.getUri(), experiment.getName()))));
        report.getVariables().add(new ResolvedItem(HEIGHT)
                .setStatus(ResolutionStatus.FOUND)
                .setMatches(List.of(new ResourceReference(height.getUri(), height.getName()))));
        for (ScientificObjectModel plot : plots) {
            report.getScientificObjects().add(new ResolvedItem(plot.getName())
                    .setStatus(ResolutionStatus.FOUND)
                    .setMatches(List.of(new ResourceReference(plot.getUri(), plot.getName()))));
        }
        session.setReport(report);
        return session;
    }

    private DataPoint point(int plot, int row, String value) {
        return new DataPoint()
                .setSheet(SHEET)
                .setRowNumber(row)
                .setObjectName(plots.get(plot).getName())
                .setDate(LocalDate.of(2024, 6, 1))
                .setVariableKey(HEIGHT)
                .setRawValue(value);
    }

    private Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(DataBulkImport.EXPERIMENT, experiment.getUri().toString());
        values.put(DataBulkImport.PROVENANCE_NAME, "ai-import test");
        return values;
    }

    private long count(String collection) {
        return getMongoDBService().getDatabase().getCollection(collection).countDocuments();
    }

    //#endregion
}
