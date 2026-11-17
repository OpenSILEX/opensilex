//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * What the columns of a file say for the fields of the forms: text of several columns in one field,
 * and what the rows of an experiment or a project add up to.
 *
 * @author Arnaud Charleroy
 */
public class FieldValuesTest {

    private static List<DescribedSheet> described(SheetStructure sheet, SheetDescription description) {
        List<DescribedSheet> sheets = new ArrayList<>();
        sheets.add(DescribedSheet.of(sheet, description));
        return sheets;
    }

    @Test
    public void severalColumnsAreJoinedEachWithItsNameAndASingleOneStandsAlone() {
        SheetStructure data = sheet("s", Arrays.asList("remarque", "sol", "note"), row("bord", "argileux", ""));

        assertEquals("remarque: bord ; sol: argileux",
                FieldValues.joined(data, data.getRows().get(0), Arrays.asList("remarque", "sol", "note")).replace(
                        " ; note: ", ""));
        assertEquals("bord", FieldValues.joined(data, data.getRows().get(0), Arrays.asList("remarque")));
        assertNull("nothing to say", FieldValues.joined(data, data.getRows().get(0), Arrays.asList("note")));
    }

    @Test
    public void anEmptyCellOfSeveralColumnsAddsNothing() {
        SheetStructure data = sheet("s", Arrays.asList("remarque", "sol"), row("", "argileux"));

        assertEquals("sol: argileux",
                FieldValues.joined(data, data.getRows().get(0), Arrays.asList("remarque", "sol")));
    }

    @Test
    public void theCommentsOfAFacilityAreJoinedAndAddUpOverItsRows() {
        SheetStructure data = sheet("sites", Arrays.asList("nom", "note1", "note2"),
                row("Nord", "plat", "venté"), row("Nord", "plat", "venté"), row("Sud", "", "humide"));
        SheetDescription description = new SheetDescription("sites").setKind(SheetKind.FACILITIES);
        description.getRoles().put("nom", DescribedRole.LOCATION);
        description.getRoles().put("note1", DescribedRole.COMMENT);
        description.getRoles().put("note2", DescribedRole.COMMENT);

        Map<String, String> comments = FieldValues.commentsOf(described(data, description), SheetKind.FACILITIES,
                DescribedRole.LOCATION);

        assertEquals("note1: plat ; note2: venté", comments.get("Nord"));
        assertEquals("a repeated row says nothing more", "note2: humide", comments.get("Sud"));
    }

    @Test
    public void theColumnsOfAnExperimentFeedItsFormFields() {
        SheetStructure data = sheet("plan", Arrays.asList("parcelle", "essai", "but", "contexte", "debut", "fin"),
                row("P01", "Essai A", "Mesurer", "Parcelle", "2026-03-01", ""),
                row("P02", "Essai A", "Mesurer", "Vigne", "", "31/10/2026"),
                row("P03", "", "ignoré", "ignoré", "2020-01-01", ""));
        SheetDescription description = new SheetDescription("plan").setKind(SheetKind.OBJECTS);
        description.getRoles().put("parcelle", DescribedRole.OBJECT);
        description.getRoles().put("essai", DescribedRole.TRIAL);
        description.getRoles().put("but", DescribedRole.EXPERIMENT_OBJECTIVE);
        description.getRoles().put("contexte", DescribedRole.EXPERIMENT_DESCRIPTION);
        description.getRoles().put("debut", DescribedRole.EXPERIMENT_START);
        description.getRoles().put("fin", DescribedRole.EXPERIMENT_END);

        Map<String, Map<String, String>> details = FieldValues.detailsOf(described(data, description),
                FormTarget.Entity.EXPERIMENT);

        assertEquals(1, details.size());
        Map<String, String> xp = details.get("Essai A");
        assertEquals("the same objective said twice is said once", "Mesurer", xp.get("objective"));
        assertEquals("what each row says, added up", "Parcelle ; Vigne", xp.get("description"));
        assertEquals("2026-03-01", xp.get("start_date"));
        assertEquals("the first value that is not empty stands", "2026-03-01", xp.get("start_date"));
        assertEquals("a date in the day-first form is read", "2026-10-31", xp.get("end_date"));
    }

    @Test
    public void aDateThatIsNoneAndAFieldOfAnotherEntityAreLeftOut() {
        SheetStructure data = sheet("plan", Arrays.asList("parcelle", "projet", "debut", "court"),
                row("P01", "Projet X", "bientôt", "PX"));
        SheetDescription description = new SheetDescription("plan").setKind(SheetKind.OBJECTS);
        description.getRoles().put("parcelle", DescribedRole.OBJECT);
        description.getRoles().put("projet", DescribedRole.PROJECT);
        description.getRoles().put("debut", DescribedRole.PROJECT_START);
        description.getRoles().put("court", DescribedRole.PROJECT_SHORTNAME);

        Map<String, String> project = FieldValues.detailsOf(described(data, description), FormTarget.Entity.PROJECT)
                .get("Projet X");

        assertFalse(project.containsKey("start_date"));
        assertEquals("PX", project.get("shortname"));
        assertEquals("nothing is said of an experiment", 0,
                FieldValues.detailsOf(described(data, description), FormTarget.Entity.EXPERIMENT).size());
    }

    @Test
    public void everyRoleThatFeedsAFormFieldSaysWhichAndTheOthersDoNot() {
        long feeding = Arrays.stream(DescribedRole.values()).filter(role -> role.formTarget().isPresent()).count();

        assertEquals(FormTarget.values().length, feeding);
        assertFalse(DescribedRole.COMMENT.formTarget().isPresent());
        assertEquals("start_date", DescribedRole.PROJECT_START.formTarget().orElseThrow().field());
    }
}
