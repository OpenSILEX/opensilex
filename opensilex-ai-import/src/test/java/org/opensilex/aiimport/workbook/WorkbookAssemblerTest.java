//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.workbook;

/**
 * The files of a conversation put together as one set of sheets.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookAssemblerTest {

    private static WorkbookStructure file(String name, boolean date1904, SheetStructure... sheets) {
        WorkbookStructure structure = workbook(sheets).setFileName(name).setDate1904(date1904);
        structure.getSheets().forEach(sheet -> sheet.setDate1904(date1904));
        return structure;
    }

    @SuppressWarnings("unchecked")
    private static SheetStructure sheetNamed(String name) {
        return sheet(name, List.of("plot", "x"), row("P01", "1"));
    }

    @Test
    public void aSingleFileIsItselfNotACopy() {
        WorkbookStructure only = file("a.xlsx", false, sheetNamed("plan"));

        assertSame(only, WorkbookAssembler.assemble(List.of(only)));
    }

    @Test
    public void theSheetsOfEveryFileFollowOneAnother() {
        WorkbookStructure assembled = WorkbookAssembler.assemble(List.of(
                file("a.xlsx", false, sheetNamed("plan"), sheetNamed("mesures")),
                file("b.csv", false, sheetNamed("b"))));

        assertEquals(List.of("plan", "mesures", "b"), assembled.getSheetNames());
        assertEquals(List.of(0, 1, 2), List.of(assembled.getSheets().get(0).getIndex(),
                assembled.getSheets().get(1).getIndex(), assembled.getSheets().get(2).getIndex()));
        assertEquals("a.xlsx, b.csv", assembled.getFileName());
    }

    @Test
    public void aSheetWhoseNameIsTakenIsNamedAfterItsFile() {
        WorkbookStructure first = file("a.xlsx", false, sheetNamed("plan"));
        WorkbookStructure second = file("b.xlsx", false, sheetNamed("Plan"), sheetNamed("autre"));

        WorkbookStructure assembled = WorkbookAssembler.assemble(List.of(first, second));

        assertEquals(List.of("plan", "b.xlsx" + WorkbookAssembler.QUALIFIER + "Plan", "autre"),
                assembled.getSheetNames());
        assertEquals("the file's own sheet is left as it was", "Plan", second.getSheets().get(0).getName());
    }

    @Test
    public void eachSheetKeepsTheDateSystemOfItsFile() {
        WorkbookStructure assembled = WorkbookAssembler.assemble(List.of(
                file("a.xlsx", false, sheetNamed("plan")),
                file("b.xlsx", true, sheetNamed("mesures"))));

        assertFalse(assembled.getSheets().get(0).isDate1904());
        assertTrue(assembled.getSheets().get(1).isDate1904());
    }

    @Test
    public void theNamesEachFileSheetsCarryAreKnownBeforeAssembling() {
        List<List<String>> names = WorkbookAssembler.sheetNames(List.of(
                file("a.xlsx", false, sheetNamed("plan")),
                file("b.xlsx", false, sheetNamed("plan"))));

        assertEquals(List.of(List.of("plan"), List.of("b.xlsx" + WorkbookAssembler.QUALIFIER + "plan")), names);
    }
}
