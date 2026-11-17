//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.binding.ImportDecisions;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.CREATION_DATE;
import static org.opensilex.graphimport.interpret.InterpretFixtures.GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.SO;
import static org.opensilex.graphimport.interpret.InterpretFixtures.services;
import static org.opensilex.graphimport.interpret.InterpretFixtures.sheet;

/**
 * A whole workbook interpreted with the module's own sources, without a triplestore.
 *
 * @author Arnaud Charleroy
 */
public class InterpreterTest {

    private final Interpreter interpreter = new Interpreter(services(), new GraphImportModule().getEvidenceSources());

    @Test
    public void aSheetOfPlotsIsUnderstoodFromItsNameHeadersAndValues() throws Exception {
        SheetStructure plots = sheet("Parcelles", List.of("nom", "Date de création", "parent", "hauteur"),
                new String[]{"P1", "2026-04-03", null, "1,5"},
                new String[]{"P2", "2026-04-04", "P1", "2,5"},
                new String[]{"P3", "2026-04-05", "P1", "3,5"});

        SheetInterpretation sheet = interpret(plots).sheets().get(0);

        assertEquals(PLOT, sheet.sheetClass().chosen());
        assertEquals(Choice.Status.SUGGESTED, sheet.sheetClass().status());
        assertEquals(Integer.valueOf(0), sheet.identifier().chosen());
        assertSuggested(NAME, sheet.columns().get(0));
        assertSuggested(CREATION_DATE, sheet.columns().get(1));
        assertSuggested(IS_PART_OF, sheet.columns().get(2));
        assertEquals(Choice.Status.UNDECIDED, sheet.columns().get(3).property().status());
        assertTrue(sheet.columns().get(0).identifier());
    }

    @Test
    public void twoColumnsSuggestedForOneSingleValuedPropertyGoBackToTheUser() throws Exception {
        SheetStructure plots = sheet("Parcelles", List.of("nom", "name", "rdfs:comment", "commentaire"),
                new String[]{"P1", "Plot 1", "a", "b"},
                new String[]{"P2", "Plot 2", "c", "d"});

        List<ColumnInterpretation> columns = interpret(plots).sheets().get(0).columns();

        assertEquals(Choice.Status.UNDECIDED, columns.get(0).property().status());
        assertEquals(Choice.Status.UNDECIDED, columns.get(1).property().status());
        assertEquals("note.duplicateSingle", columns.get(1).property().notes().get(0).getKey());
        assertEquals(Choice.Status.DECIDED, columns.get(2).property().status());
        assertEquals(COMMENT, columns.get(2).property().chosen());
        assertEquals(Choice.Status.UNDECIDED, columns.get(3).property().status());
    }

    @Test
    public void aTypeColumnDecidesTheClassOfTheSheet() throws Exception {
        SheetStructure objects = sheet("objets", List.of("a", "nom"),
                new String[]{"vocabulary:Plot", "P1"},
                new String[]{"Parcelle", "P2"});

        SheetInterpretation sheet = interpret(objects).sheets().get(0);

        assertEquals(PLOT, sheet.sheetClass().chosen());
        assertEquals(Choice.Status.DECIDED, sheet.sheetClass().status());
        assertEquals(Set.of(PLOT), sheet.rowClasses());
        assertTrue(sheet.columns().get(0).typeColumn());
        assertEquals(List.of("note.noExperiment"), interpret(objects).notes().stream().map(note -> note.getKey()).toList());
    }

    @Test
    public void typesThatAreNotClassesAreReported() throws Exception {
        SheetStructure objects = sheet("objets", List.of("rdf:type", "nom"),
                new String[]{"vocabulary:Plot", "P1"},
                new String[]{"Vigne", "P2"});

        SheetInterpretation sheet = interpret(objects).sheets().get(0);

        assertNull(sheet.sheetClass().chosen());
        assertEquals("note.unresolvedTypes", sheet.sheetClass().notes().get(0).getKey());
        assertEquals("Vigne", sheet.sheetClass().notes().get(0).getParams().get("values"));
    }

    @Test
    public void withoutAClassOnlyPredicateHeadersTellAnything() throws Exception {
        SheetStructure unknown = sheet("feuille 1", List.of("vocabulary:hasCreationDate", "truc", "ex:machin"),
                new String[]{"2026-04-03", "x", "y"});

        SheetInterpretation sheet = interpret(unknown).sheets().get(0);

        assertNull(sheet.classView());
        assertEquals(Choice.Status.DECIDED, sheet.columns().get(0).property().status());
        assertEquals(CREATION_DATE, sheet.columns().get(0).property().chosen());
        assertEquals(Choice.Status.UNDECIDED, sheet.columns().get(1).property().status());
        assertEquals("note.unknownPrefix", sheet.columns().get(2).property().notes().get(0).getKey());
    }

    @Test
    public void theUserDecisionsReplaceTheSuggestionsButKeepTheEvidence() throws Exception {
        SheetStructure plots = sheet("Parcelles", List.of("nom", "remarque", "vocabulary:hasGermplasm"),
                new String[]{"P1", "a", "g"});
        ImportDecisions decisions = new ImportDecisions(Map.of("Parcelles",
                new ImportDecisions.SheetDecisions(SO, 1, Map.of(1, COMMENT, 0, URI.create("http://not/accepted")))));

        SheetInterpretation sheet = interpreter.interpret(workbook(plots), decisions, null).sheets().get(0);

        assertEquals(SO, sheet.sheetClass().chosen());
        assertEquals(Choice.Status.CHOSEN, sheet.sheetClass().status());
        assertFalse(sheet.sheetClass().candidates().isEmpty());
        assertEquals(Integer.valueOf(1), sheet.identifier().chosen());
        assertEquals(Choice.Status.CHOSEN, sheet.columns().get(1).property().status());
        assertEquals(COMMENT, sheet.columns().get(1).property().chosen());
        assertEquals(NAME, sheet.columns().get(0).property().candidates().get(0).target());
        assertTrue(sheet.columns().get(0).property().status() != Choice.Status.CHOSEN);
        assertEquals("note.notAccepted", interpreter.interpret(workbook(sheet("Parcelles", List.of("vocabulary:hasNothing"),
                new String[]{"x"})), decisions, null).sheets().get(0).columns().get(0).property().notes().get(0).getKey());
    }

    @Test
    public void aColumnLeftOutHasNoPropertyButKeepsItsCandidates() throws Exception {
        SheetStructure plots = sheet("Parcelles", List.of("nom", "remarque"), new String[]{"P1", "a"});
        ImportDecisions decisions = new ImportDecisions(Map.of("Parcelles",
                new ImportDecisions.SheetDecisions(SO, 0, Map.of(), Map.of(), Set.of(1))));

        SheetInterpretation sheet = interpreter.interpret(workbook(plots), decisions, null).sheets().get(0);

        Choice<URI> remark = sheet.columns().get(1).property();
        assertEquals(Choice.Status.LEFT_OUT, remark.status());
        assertNull(remark.chosen());
        assertFalse("the user can take the column back", remark.candidates().isEmpty());
        assertTrue(sheet.columns().get(0).property().status() != Choice.Status.LEFT_OUT);
    }

    @Test
    public void aSheetThatIsNotATableIsListedWithANote() throws Exception {
        SheetStructure readme = new SheetStructure().setName("Lisez-moi").setHeaders(List.of()).setRows(List.of())
                .setTabular(false);

        SheetInterpretation sheet = interpret(readme).sheets().get(0);

        assertEquals("note.notATable", sheet.notes().get(0).getKey());
        assertTrue(sheet.columns().isEmpty());
    }

    @Test
    public void theSameFileGivesTheSameAnswer() throws Exception {
        SheetStructure plots = sheet("Parcelles", List.of("nom", "germplasme"),
                new String[]{"P1", "G1"}, new String[]{"P2", "G2"});

        assertEquals(summary(interpret(plots)), summary(interpret(plots)));
        assertFalse(GERMPLASM.equals(interpret(plots).sheets().get(0).sheetClass().chosen()));
    }

    /** Every answer, status and score of an interpretation, as text. */
    private static String summary(Interpretation interpretation) {
        StringBuilder text = new StringBuilder();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            text.append(sheet.name()).append(summary(sheet.sheetClass())).append(summary(sheet.identifier()));
            sheet.columns().forEach(column -> text.append(column.header()).append(summary(column.property())));
        }
        return text.toString();
    }

    private static String summary(Choice<?> choice) {
        StringBuilder text = new StringBuilder("[" + choice.chosen() + " " + choice.status());
        choice.candidates().forEach(candidate -> text.append(' ').append(candidate.target()).append('=')
                .append(candidate.score()));
        return text.append(']').toString();
    }

    private Interpretation interpret(SheetStructure... sheets) throws Exception {
        return interpreter.interpret(workbook(sheets), ImportDecisions.NONE, null);
    }

    private static WorkbookStructure workbook(SheetStructure... sheets) {
        return new WorkbookStructure().setSheets(List.of(sheets));
    }

    private static void assertSuggested(URI property, ColumnInterpretation column) {
        assertEquals(column.header(), property, column.property().chosen());
        assertEquals(column.header(), Choice.Status.SUGGESTED, column.property().status());
    }
}
