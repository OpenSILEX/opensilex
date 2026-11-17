//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.workbook;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.binding.ImportDecisions.DeclaredResource;
import org.opensilex.graphimport.binding.ImportDecisions.SheetDecisions;
import org.opensilex.graphimport.interpret.InterpretFixtures;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.SO;

/**
 * What the user adds in the assistant becomes columns and sheets the rest of the module reads as it reads
 * the files, and the files' sheets are never changed.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookAdditionsTest {

    private static final SheetStructure OBJECTS = InterpretFixtures.sheet("Placettes", List.of("nom", "remarque"),
            new String[]{"P1", "a"}, new String[]{"P2"});

    @Test
    public void withNothingAddedTheFilesAreReadAsTheyAre() {
        WorkbookStructure files = workbook(OBJECTS);

        WorkbookAdditions.ReadWorkbook read = WorkbookAdditions.apply(files, ImportDecisions.NONE);

        assertSame(files, read.workbook());
        assertFalse(read.isGiven("Placettes", 1));
    }

    @Test
    public void aGivenValueIsAColumnEveryRowHolds() {
        WorkbookStructure files = workbook(OBJECTS);
        ImportDecisions decisions = new ImportDecisions(Map.of("Placettes",
                new SheetDecisions(SO, 0, Map.of(), Map.of(), Set.of(), Map.of(IS_PART_OF, " Bloc A "))));

        WorkbookAdditions.ReadWorkbook read = WorkbookAdditions.apply(files, decisions);
        SheetStructure sheet = read.workbook().getSheet("Placettes").orElseThrow();

        assertEquals(List.of("nom", "remarque", IS_PART_OF.toString()), sheet.getHeaders());
        assertEquals(List.of(List.of("P1", "a", "Bloc A"), List.of("P2", "", "Bloc A")), sheet.getRows());
        assertTrue(read.isGiven("Placettes", 2));
        assertFalse(read.isGiven("Placettes", 1));
        assertEquals("the file's sheet is untouched", List.of("nom", "remarque"), OBJECTS.getHeaders());
    }

    @Test
    public void declaredResourcesAreTheRowsOfASheetOfTheirClass() {
        DeclaredResource blockA = new DeclaredResource("a", PLOT, Map.of(NAME, List.of("Bloc A")));
        DeclaredResource blockB = new DeclaredResource("b", PLOT, Map.of(COMMENT, List.of("nord"),
                NAME, List.of("Bloc B")));

        WorkbookAdditions.ReadWorkbook read = WorkbookAdditions.apply(workbook(OBJECTS),
                new ImportDecisions(Map.of(), List.of(blockA, blockB)));
        SheetStructure sheet = read.workbook().getSheet("[Plot]").orElseThrow();

        assertEquals("the name comes first", List.of(NAME.toString(), COMMENT.toString()), sheet.getHeaders());
        assertEquals(List.of(List.of("Bloc A", ""), List.of("Bloc B", "nord")), sheet.getRows());
        assertTrue(read.isDeclared("[Plot]"));
        assertEquals(List.of("a", "b"), read.declaredRowsOf("[Plot]"));
        assertEquals(PLOT, read.decisions().of("[Plot]").classUri());
        assertEquals(Integer.valueOf(0), read.decisions().of("[Plot]").identifierColumn());
    }

    @Test
    public void aPropertyWithSeveralValuesTakesAsManyColumns() {
        DeclaredResource block = new DeclaredResource("a", PLOT, Map.of(NAME, List.of("Bloc A"),
                COMMENT, List.of("nord", "sec")));

        SheetStructure sheet = WorkbookAdditions.apply(workbook(OBJECTS), new ImportDecisions(Map.of(), List.of(block)))
                .workbook().getSheet("[Plot]").orElseThrow();

        assertEquals(List.of(NAME.toString(), COMMENT.toString(), COMMENT.toString()), sheet.getHeaders());
        assertEquals(List.of(List.of("Bloc A", "nord", "sec")), sheet.getRows());
    }

    @Test
    public void anAddedSheetNeverTakesTheNameOfASheetOfTheFiles() {
        SheetStructure taken = InterpretFixtures.sheet("[Plot]", List.of("nom"), new String[]{"X"});
        DeclaredResource block = new DeclaredResource("a", PLOT, Map.of(NAME, List.of("Bloc A")));

        WorkbookAdditions.ReadWorkbook read = WorkbookAdditions.apply(workbook(taken),
                new ImportDecisions(Map.of(), List.of(block)));

        assertEquals(List.of("[Plot]", "[Plot] 2"), read.workbook().getSheetNames());
        assertTrue(read.isDeclared("[Plot] 2"));
        assertFalse(read.isDeclared("[Plot]"));
    }

    private static WorkbookStructure workbook(SheetStructure... sheets) {
        return new WorkbookStructure().setFileName("essai.xlsx").setSheets(List.of(sheets));
    }
}
