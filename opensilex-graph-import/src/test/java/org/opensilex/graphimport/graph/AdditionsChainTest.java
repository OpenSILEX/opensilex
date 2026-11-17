//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.junit.Test;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.binding.ImportDecisions.DeclaredResource;
import org.opensilex.graphimport.binding.ImportDecisions.SheetDecisions;
import org.opensilex.graphimport.graph.GraphFixtures.SheetSpec;
import org.opensilex.graphimport.interpret.Choice;
import org.opensilex.graphimport.interpret.ColumnInterpretation;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.Interpreter;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.workbook.WorkbookAdditions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.opensilex.graphimport.graph.GraphFixtures.services;
import static org.opensilex.graphimport.graph.GraphFixtures.workbook;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.CREATION_DATE;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;

/**
 * A value given for every row and a resource declared in the assistant go through the interpretation,
 * the merge and the links as the files do: the rows point to the declared resource by its name.
 *
 * @author Arnaud Charleroy
 */
public class AdditionsChainTest {

    private static final SheetSpec SUBPLOTS = SheetSpec.of("Sous-parcelles", List.of("nom", "remarque"),
            new String[]{"SP1", "a"},
            new String[]{"SP2", "b"});
    private static final SheetDecisions GIVEN_BLOCK = new SheetDecisions(PLOT, 0, Map.of(0, NAME, 1, COMMENT), Map.of(),
            Set.of(), Map.of(IS_PART_OF, "Bloc A"));
    private static final DeclaredResource BLOCK = new DeclaredResource("bloc", PLOT, Map.of(NAME, List.of("Bloc A")));

    @Test
    public void aGivenValueIsReadAsAPropertyTheFileSays() throws Exception {
        Read read = read(new ImportDecisions(Map.of("Sous-parcelles", GIVEN_BLOCK)));

        ColumnInterpretation given = read.sheet("Sous-parcelles").columns().get(2);

        assertEquals(IS_PART_OF, given.property().chosen());
        assertEquals(Choice.Status.DECIDED, given.property().status());
    }

    @Test
    public void withoutTheResourceTheGivenLinkPointsToNothing() throws Exception {
        assertEquals(List.of(new GraphLinks.ColumnLink("Sous-parcelles", 2, IS_PART_OF, GraphLinks.Kind.UNRESOLVED,
                null, null, 2)), read(new ImportDecisions(Map.of("Sous-parcelles", GIVEN_BLOCK))).links().columnLinks());
    }

    @Test
    public void onceDeclaredTheRowsPointToTheResource() throws Exception {
        Read read = read(new ImportDecisions(Map.of("Sous-parcelles", GIVEN_BLOCK), List.of(BLOCK)));

        assertEquals(List.of(new GraphLinks.ColumnLink("Sous-parcelles", 2, IS_PART_OF, GraphLinks.Kind.ROW,
                "[Plot]", null, 2)), read.links().columnLinks());
    }

    @Test
    public void aRequiredPropertyNoColumnGivesIsSaid() throws Exception {
        SheetSpec remarks = SheetSpec.of("Remarques", List.of("remarque", "date"), new String[]{"bord", "2026-04-03"});
        Read read = read(new ImportDecisions(Map.of("Remarques",
                new SheetDecisions(PLOT, null, Map.of(0, COMMENT, 1, CREATION_DATE)))), remarks);

        assertEquals(List.of(NAME), read.sheet("Remarques").unfilledRequired());
        assertEquals(List.of(), read(new ImportDecisions(Map.of("Sous-parcelles", GIVEN_BLOCK)))
                .sheet("Sous-parcelles").unfilledRequired());
    }

    /** The interpretation of a workbook with what the user added, and where its links land. */
    private record Read(Interpretation interpretation, GraphLinks links) {

        SheetInterpretation sheet(String name) {
            return interpretation.sheets().stream().filter(sheet -> sheet.name().equals(name)).findFirst()
                    .orElseThrow();
        }
    }

    private static Read read(ImportDecisions decisions, SheetSpec... sheets) throws Exception {
        WorkbookStructure files = workbook(sheets.length == 0 ? new SheetSpec[]{SUBPLOTS} : sheets);
        WorkbookAdditions.ReadWorkbook read = WorkbookAdditions.apply(files, decisions);
        InterpretationServices services = services();
        Interpretation interpretation = new Interpreter(services, new GraphImportModule().getEvidenceSources())
                .interpret(read.workbook(), read.decisions(), null);
        ImportGraph graph = new GraphBuilder(services).build(read.workbook(), interpretation, read.decisions());
        return new Read(interpretation, GraphLinks.of(graph, interpretation));
    }
}
