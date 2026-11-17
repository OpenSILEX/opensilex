//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.flatSheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * Observations are read as the file states them, and a row that cannot be placed is counted, not
 * guessed at.
 *
 * @author Arnaud Charleroy
 */
public class DataReaderTest {

    @Test
    public void everyNonMissingMeasureOfAnObservationRowIsAPoint() {
        DataReader.Read read = DataReader.read(sheets(flatSheet(), null));

        assertEquals("4 rows, two variables, one of them 'NA' once", 7, read.points().size());
        DataPoint first = read.points().get(0);
        assertEquals("essai", first.getSheet());
        assertEquals(2, first.getRowNumber());
        assertEquals("P01", first.getObjectName());
        assertEquals(DataPoint.TargetKind.SCIENTIFIC_OBJECT, first.getTargetKind());
        assertEquals(LocalDate.of(2026, 6, 12), first.getDate());
        assertEquals("hauteur_cm", first.getVariableKey());
        assertEquals("84", first.getRawValue());
        assertTrue(read.anomalies().isEmpty());
    }

    @Test
    public void aColumnTheUserDeclaredADateIsOneWhateverItsHeaderSaysAndHoweverItsCellsAreWritten() {
        SheetStructure sheet = sheet("mesures", Arrays.asList("plot_id", "jour", "hauteur_cm"),
                row("P01", "46185", "84"),
                row("P01", "12/07/2026", "90"),
                row("P01", "2026-08-01 10:30", "95"));
        FileDescription description = new FileDescription();
        description.sheet("mesures").setKind(SheetKind.DATA).getRoles().put("jour", DescribedRole.DATE);

        DataReader.Read read = DataReader.read(sheets(sheet, description));

        assertEquals(3, read.points().size());
        assertEquals(LocalDate.of(2026, 6, 12), read.points().get(0).getDate());
        assertEquals(LocalDate.of(2026, 7, 12), read.points().get(1).getDate());
        assertEquals(LocalDate.of(2026, 8, 1), read.points().get(2).getDate());
    }

    @Test
    public void rowsWithoutATargetOrAReadableDateAreLeftOutCountedAndReported() {
        SheetStructure sheet = sheet("mesures", Arrays.asList("plot_id", "date", "hauteur_cm"),
                row("P01", "2026-06-12", "84"),
                row("", "2026-06-12", "70"),
                row("P02", "demain", "71"),
                row("P03", "", "72"),
                row("P04", "2026-06-12", ""));

        DataReader.Read read = DataReader.read(sheets(sheet, null));

        assertEquals("only the row that can be placed", 1, read.points().size());
        assertEquals(2, read.anomalies().size());
        assertEquals(DataReader.NO_TARGET, read.anomalies().get(0).getKey());
        assertEquals("1", read.anomalies().get(0).getParams().get("count"));
        assertEquals("3", read.anomalies().get(0).getParams().get("rows"));
        assertEquals(DataReader.NO_DATE, read.anomalies().get(1).getKey());
        assertEquals("2", read.anomalies().get(1).getParams().get("count"));
        assertEquals("rows 4 and 5, as the spreadsheet numbers them", "4, 5", read.anomalies().get(1).getParams().get("rows"));
    }

    @Test
    public void aRowWithNoMeasureAtAllIsNeitherAPointNorAnAnomaly() {
        SheetStructure sheet = sheet("mesures", Arrays.asList("plot_id", "date", "hauteur_cm"), row("", "", ""));

        DataReader.Read read = DataReader.read(sheets(sheet, null));

        assertTrue(read.points().isEmpty());
        assertTrue(read.anomalies().isEmpty());
    }

    @Test
    public void observationsOfAFacilityAreFiledAgainstTheFacility() {
        SheetStructure weather = sheet("meteo", Arrays.asList("station", "date", "pluie_mm"),
                row("Bellegarde", "2026-06-12", "3.2"));
        FileDescription description = new FileDescription();
        description.sheet("meteo").setKind(SheetKind.DATA);

        DataReader.Read read = DataReader.read(sheets(weather, description));

        assertEquals(1, read.points().size());
        assertEquals(DataPoint.TargetKind.FACILITY, read.points().get(0).getTargetKind());
        assertEquals("Bellegarde", read.points().get(0).getObjectName());
    }

    @Test
    public void theObserverIsKeptWhenAColumnNamesOne() {
        SheetStructure sheet = sheet("mesures", Arrays.asList("plot_id", "date", "observateur", "hauteur_cm"),
                row("P01", "2026-06-12", "Alice", "84"), row("P02", "2026-06-12", "", "80"));

        DataReader.Read read = DataReader.read(sheets(sheet, null));

        assertEquals("Alice", read.points().get(0).getObserver());
        assertNull(read.points().get(1).getObserver());
    }

    @Test
    public void theDateSystemOfTheSheetsFileDecidesWhatASerialNumberIs() {
        SheetStructure sheet = sheet("mesures", Arrays.asList("plot_id", "date", "hauteur_cm"),
                row("P01", "44000", "84"));

        LocalDate in1900 = DataReader.read(sheets(sheet, null)).points().get(0).getDate();
        sheet.setDate1904(true);
        LocalDate in1904 = DataReader.read(sheets(sheet, null)).points().get(0).getDate();

        assertEquals(LocalDate.of(2020, 6, 18), in1900);
        assertEquals("the 1904 system counts four years and a day less", LocalDate.of(2024, 6, 19), in1904);
    }

    @Test
    public void aSheetWithAProblemIsNotRead() {
        SheetStructure sheet = sheet("mesures", Arrays.asList("date", "hauteur_cm"), row("2026-06-12", "84"));
        FileDescription description = new FileDescription();
        description.sheet("mesures").setKind(SheetKind.DATA);

        assertTrue("no target: the observations cannot be filed",
                DataReader.read(sheets(sheet, description)).points().isEmpty());
    }

    private static List<DescribedSheet> sheets(SheetStructure sheet, FileDescription description) {
        List<DescribedSheet> sheets = new ArrayList<>();
        sheets.add(DescribedSheet.of(sheet, description == null ? null : description.find(sheet.getName()).orElse(null)));
        return sheets;
    }
}
