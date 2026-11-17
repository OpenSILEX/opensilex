//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.flat;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.planAndMaterial;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.workbook;

/**
 * An object is its name across the workbook: described by several sheets, it is created once.
 *
 * @author Arnaud Charleroy
 */
public class ObjectMergerTest {

    @Test
    public void aFlatSheetGivesOneObjectPerDistinctName() {
        ObjectMerger.Merged merged = ObjectMerger.merge(described(flat(), null));

        assertEquals(Arrays.asList("P01", "P02"), names(merged));
        ObjectRow first = merged.rows().get(0);
        assertEquals("Syrah", first.getGermplasm());
        assertEquals("T1", first.getFactorLevel());
        assertEquals("essai", first.getSheet());
        assertEquals("the first row naming the object", 2, first.getRow());
        assertTrue(merged.anomalies().isEmpty());
    }

    @Test
    public void twoSheetsDescribingTheSameObjectsGiveOneObjectEachWithBothSheetsInformation() {
        ObjectMerger.Merged merged = ObjectMerger.merge(described(planAndMaterial(), null));

        assertEquals("P03 is in one sheet only and is still an object",
                Arrays.asList("P01", "P02", "P03"), names(merged));
        ObjectRow p01 = merged.rows().get(0);
        assertEquals("attached to the first sheet that names it", "plan", p01.getSheet());
        assertEquals("B1", p01.cell("bloc"));
        assertEquals("T1", p01.getFactorLevel());
        assertEquals("the plant material comes from the other sheet", "Syrah", p01.getGermplasm());
        assertEquals("under the other sheet's header, qualified", "Syrah",
                p01.cell("materiel" + ObjectMerger.QUALIFIER + "variete"));
        assertEquals("an object only the second sheet knows is attached to it", "materiel",
                merged.rows().get(2).getSheet());
    }

    @Test
    public void theColumnsOfTheOtherSheetsHaveTheirOwnTargetUnderTheSheetTheObjectIsAttachedTo() {
        ObjectMerger.Merged merged = ObjectMerger.merge(described(planAndMaterial(), null));

        Map<String, String> targets = merged.targetsBySheet().get("plan");
        assertEquals(ObjectTargets.NAME, targets.get("plot_id"));
        assertEquals(ObjectTargets.PARENT, targets.get("bloc"));
        assertEquals(ObjectTargets.FACTOR_LEVEL, targets.get("traitement"));
        assertEquals(ObjectTargets.GERMPLASM, targets.get("materiel" + ObjectMerger.QUALIFIER + "variete"));
    }

    @Test
    public void theSheetThatOnlyDescribesObjectsIsPreferredToTheOneThatAlsoHoldsObservations() {
        WorkbookStructure workbook = workbook(
                sheet("mesures", Arrays.asList("plot_id", "variete", "date", "hauteur_cm"),
                        row("P01", "Syrah", "2026-06-12", "84")),
                sheet("plan", Arrays.asList("plot_id", "bloc"), row("P01", "B1")));

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, null));

        assertEquals("plan", merged.rows().get(0).getSheet());
    }

    @Test
    public void twoDifferentValuesForTheSameInformationRefuseTheObject() {
        WorkbookStructure workbook = workbook(
                sheet("plan", Arrays.asList("plot_id", "variete"), row("P01", "Syrah"), row("P02", "Merlot")),
                sheet("materiel", Arrays.asList("plot_id", "variete"), row("P01", "Grenache"), row("P02", "merlot")));

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, null));

        ObjectRow p01 = merged.rows().get(0);
        assertEquals(1, p01.getProblems().size());
        assertEquals(ObjectMerger.CONFLICT, p01.getProblems().get(0).getKey());
        assertEquals("both sheets and both rows are named", "plan", p01.getProblems().get(0).getParams().get("sheet"));
        assertEquals("materiel", p01.getProblems().get(0).getParams().get("otherSheet"));
        assertEquals("Syrah", p01.getGermplasm());
        assertTrue("a difference of case alone is not a contradiction", merged.rows().get(1).getProblems().isEmpty());
        assertEquals(1, merged.anomalies().size());
    }

    @Test
    public void severalColumnsOfLevelsAreAllKeptAndEachNamesItsFactor() {
        WorkbookStructure workbook = workbook(sheet("plan",
                Arrays.asList("plot_id", "traitement", "modalite_irrigation"), row("P01", "T1", "Sec")));

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, null));

        ObjectRow p01 = merged.rows().get(0);
        assertEquals("T1", p01.getFactorLevel());
        assertEquals("Sec", p01.cell("modalite_irrigation"));
        Map<String, String> targets = merged.targetsBySheet().get("plan");
        assertEquals(ObjectTargets.FACTOR_LEVEL, targets.get("traitement"));
        assertEquals(ObjectTargets.FACTOR_LEVEL, targets.get("modalite_irrigation"));
    }

    @Test
    public void aPropertyIsWrittenToTheOneTheUserChoseAndAColumnLeftUnchosenIsNot() {
        WorkbookStructure workbook = workbook(sheet("plan",
                Arrays.asList("plot_id", "ecartement", "remarque"), row("P01", "2.5", "bord")));
        SheetDescription said = new SheetDescription("plan");
        said.getRoles().put("ecartement", DescribedRole.OBJECT_PROPERTY);
        said.getProperties().put("ecartement", "http://example.org/hasSpacing");
        said.getRoles().put("remarque", DescribedRole.OBJECT_PROPERTY);
        FileDescription description = new FileDescription();
        description.getSheets().put("plan", said);

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, description));

        Map<String, String> targets = merged.targetsBySheet().get("plan");
        assertEquals("http://example.org/hasSpacing", targets.get("ecartement"));
        assertEquals(ObjectTargets.NONE, targets.get("remarque"));
    }

    @Test
    public void aPositionColumnTakesItsAxisFromItsHeader() {
        WorkbookStructure workbook = workbook(sheet("plan",
                Arrays.asList("plot_id", "plot_x", "plot_y"), row("P01", "1", "2")));

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, null));

        ObjectRow p01 = merged.rows().get(0);
        assertEquals("1", p01.getX());
        assertEquals("2", p01.getY());
        assertEquals(ObjectTargets.X, merged.targetsBySheet().get("plan").get("plot_x"));
        assertEquals(ObjectTargets.Y, merged.targetsBySheet().get("plan").get("plot_y"));
    }

    @Test
    public void rowsWithoutAnIdentifierAreNotObjects() {
        WorkbookStructure workbook = workbook(sheet("plan", Arrays.asList("plot_id", "bloc"),
                row("P01", "B1"), row("", "B2"), row("  ", "B3")));

        assertEquals(Arrays.asList("P01"), names(ObjectMerger.merge(described(workbook, null))));
    }

    @Test
    public void aSheetWithAProblemTakesNoPartAndAFlatSheetOfObservationsAloneStillDoes() {
        WorkbookStructure workbook = workbook(
                sheet("notes", Arrays.asList("couleur"), row("rouge")),
                sheet("mesures", Arrays.asList("plot_id", "date", "hauteur_cm"), row("P01", "2026-06-12", "84")));
        FileDescription description = new FileDescription();
        description.sheet("mesures").setKind(SheetKind.OBJECTS_AND_DATA);

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, description));

        assertEquals("objects and data needs the date, the variable and the identifier: it has them",
                Arrays.asList("P01"), names(merged));
        assertNull(merged.targetsBySheet().get("notes"));
        assertNotNull(merged.targetsBySheet().get("mesures"));
    }

    @Test
    public void moreThanTenContradictionsAreSummarisedButEveryObjectKeepsItsOwn() {
        List<List<String>> plan = new ArrayList<>();
        List<List<String>> material = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            plan.add(row("P" + i, "Syrah"));
            material.add(row("P" + i, "Grenache"));
        }
        WorkbookStructure workbook = workbook(
                new SheetStructure().setName("plan").setHeaders(Arrays.asList("plot_id", "variete")).setRows(plan),
                new SheetStructure().setName("materiel").setHeaders(Arrays.asList("plot_id", "variete")).setRows(material));

        ObjectMerger.Merged merged = ObjectMerger.merge(described(workbook, null));

        assertEquals(11, merged.anomalies().size());
        assertEquals(ObjectMerger.MORE_CONFLICTS, merged.anomalies().get(10).getKey());
        assertEquals("2", merged.anomalies().get(10).getParams().get("count"));
        assertTrue(merged.rows().stream().allMatch(object -> object.getProblems().size() == 1));
    }

    private static List<DescribedSheet> described(WorkbookStructure workbook, FileDescription description) {
        List<DescribedSheet> sheets = new ArrayList<>();
        for (SheetStructure sheet : workbook.getSheets()) {
            sheets.add(DescribedSheet.of(sheet,
                    description == null ? null : description.find(sheet.getName()).orElse(null)));
        }
        return sheets;
    }

    private static List<String> names(ObjectMerger.Merged merged) {
        return merged.rows().stream().map(ObjectRow::getName).collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private static List<String> keys(List<ReportMessage> messages) {
        return messages.stream().map(ReportMessage::getKey).collect(Collectors.toList());
    }
}
