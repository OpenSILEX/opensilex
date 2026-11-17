//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.junit.Test;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.graph.GraphFixtures.SheetSpec;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.Interpreter;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.graph.GraphFixtures.services;
import static org.opensilex.graphimport.graph.GraphFixtures.workbook;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.CREATION_DATE;
import static org.opensilex.graphimport.interpret.InterpretFixtures.GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.HAS_GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;

/**
 * Where the links of each column land, and which sheets describe the same resources.
 *
 * @author Arnaud Charleroy
 */
public class GraphLinksTest {

    private static final SheetSpec PLOTS = SheetSpec.of("Parcelles", List.of("nom", "Date de création", "germplasme"),
            new String[]{"P1", "03/04/2026", "Syrah"},
            new String[]{"P2", "25/04/2026", "Syrha"});
    private static final SheetSpec MORE = SheetSpec.of("Compléments", List.of("nom", "remarque", "date"),
            new String[]{"P1", "bord", "2026-04-03"},
            new String[]{"P2", "centre", "2026-05-01"});
    private static final SheetSpec SUBPLOTS = SheetSpec.of("Sous-parcelles", List.of("nom", "parent"),
            new String[]{"SP1", "P1"},
            new String[]{"SP2", "Parcelle 404"});

    @Test
    public void eachLinkColumnSaysWhereItsCellsLand() throws Exception {
        GraphLinks links = links(workbook(PLOTS, MORE, SUBPLOTS));

        assertEquals(List.of(
                new GraphLinks.ColumnLink("Parcelles", 2, HAS_GERMPLASM, GraphLinks.Kind.INSTANCE, null, GERMPLASM, 1),
                new GraphLinks.ColumnLink("Parcelles", 2, HAS_GERMPLASM, GraphLinks.Kind.UNRESOLVED, null, null, 1),
                new GraphLinks.ColumnLink("Sous-parcelles", 1, IS_PART_OF, GraphLinks.Kind.ROW, "Parcelles", null, 1),
                new GraphLinks.ColumnLink("Sous-parcelles", 1, IS_PART_OF, GraphLinks.Kind.UNRESOLVED, null, null, 1)),
                links.columnLinks());
        assertTrue("P1 is in two sheets: its links are drawn to the first", links.columnLinks().stream()
                .noneMatch(link -> "Compléments".equals(link.toSheet())));
    }

    @Test
    public void sheetsSharingIdentifiersShareResources() throws Exception {
        assertEquals(List.of(new GraphLinks.SharedResources("Parcelles", "Compléments", 2)),
                links(workbook(PLOTS, MORE, SUBPLOTS)).shared());
    }

    private static GraphLinks links(WorkbookStructure workbook) throws Exception {
        Map<String, ImportDecisions.SheetDecisions> decided = new LinkedHashMap<>();
        decided.put("Parcelles", sheet(Map.of(0, NAME, 1, CREATION_DATE, 2, HAS_GERMPLASM)));
        decided.put("Compléments", sheet(Map.of(0, NAME, 1, COMMENT, 2, CREATION_DATE)));
        decided.put("Sous-parcelles", sheet(Map.of(0, NAME, 1, IS_PART_OF)));
        ImportDecisions decisions = new ImportDecisions(decided);
        InterpretationServices services = services();
        Interpretation interpretation = new Interpreter(services, new GraphImportModule().getEvidenceSources())
                .interpret(workbook, decisions, null);
        return GraphLinks.of(new GraphBuilder(services).build(workbook, interpretation, decisions), interpretation);
    }

    private static ImportDecisions.SheetDecisions sheet(Map<Integer, URI> properties) {
        return new ImportDecisions.SheetDecisions(PLOT, 0, properties);
    }
}
