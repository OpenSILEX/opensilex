//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.flatSheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * What a sheet is taken to be when nobody has said.
 *
 * @author Arnaud Charleroy
 */
public class DescriptionDefaultsTest {

    @Test
    public void aHeaderTheDictionaryKnowsGivesItsRole() {
        SheetStructure flat = flatSheet();

        assertEquals(DescribedRole.OBJECT, DescriptionDefaults.roleOf(flat, "plot_id"));
        assertEquals(DescribedRole.GERMPLASM, DescriptionDefaults.roleOf(flat, "variete"));
        assertEquals(DescribedRole.FACTOR_LEVEL, DescriptionDefaults.roleOf(flat, "traitement"));
        assertEquals(DescribedRole.PARENT_OBJECT, DescriptionDefaults.roleOf(flat, "bloc"));
        assertEquals(DescribedRole.DATE, DescriptionDefaults.roleOf(flat, "date"));
        assertEquals(DescribedRole.COMMENT, DescriptionDefaults.roleOf(flat, "remarque"));
    }

    @Test
    public void aSheetThatNamesExperimentsAndNothingTheyHoldDescribesTheExperiments() {
        assertEquals(SheetKind.EXPERIMENTS, DescriptionDefaults.kindOf(Arrays.asList(DescribedRole.TRIAL,
                DescribedRole.EXPERIMENT_OBJECTIVE, DescribedRole.PROJECT)));
        assertEquals("observations name their experiment too", SheetKind.DATA, DescriptionDefaults.kindOf(
                Arrays.asList(DescribedRole.TRIAL, DescribedRole.DATE, DescribedRole.VARIABLE)));
    }

    @Test
    public void aNumericColumnWithAnUnknownNameIsAVariable() {
        assertEquals(DescribedRole.VARIABLE, DescriptionDefaults.roleOf(flatSheet(), "hauteur_cm"));
    }

    @Test
    public void aColumnOfWordsWithAnUnknownNameIsNotRead() {
        SheetStructure words = sheet("s", Arrays.asList("couleur"), row("rouge"), row("vert"));

        assertEquals(DescribedRole.UNREAD, DescriptionDefaults.roleOf(words, "couleur"));
    }

    @Test
    public void aHeaderThatNamesTheFactorIsTheFactorNotALevel() {
        SheetStructure levels = sheet("s", Arrays.asList("facteur", "modalite"), row("Cuivre", "T1"));

        assertEquals(DescribedRole.FACTOR, DescriptionDefaults.roleOf(levels, "facteur"));
        assertEquals(DescribedRole.FACTOR_LEVEL, DescriptionDefaults.roleOf(levels, "modalite"));
    }

    @Test
    public void theRolesPointAtTheKindOfSheet() {
        assertEquals(SheetKind.OBJECTS_AND_DATA, DescriptionDefaults.kindOf(Arrays.asList(
                DescribedRole.OBJECT, DescribedRole.GERMPLASM, DescribedRole.DATE, DescribedRole.VARIABLE)));
        assertEquals(SheetKind.DATA, DescriptionDefaults.kindOf(Arrays.asList(
                DescribedRole.OBJECT, DescribedRole.DATE, DescribedRole.VARIABLE)));
        assertEquals(SheetKind.OBJECTS, DescriptionDefaults.kindOf(Arrays.asList(
                DescribedRole.OBJECT, DescribedRole.GERMPLASM)));
        assertEquals(SheetKind.FACTORS, DescriptionDefaults.kindOf(Arrays.asList(
                DescribedRole.FACTOR, DescribedRole.FACTOR_LEVEL)));
        assertEquals(SheetKind.IGNORED, DescriptionDefaults.kindOf(Arrays.asList(
                DescribedRole.UNREAD, DescribedRole.COMMENT)));
    }

    @Test
    public void nothingInAnEmptyListOfRolesIsRead() {
        List<DescribedRole> none = Arrays.asList();

        assertEquals(SheetKind.IGNORED, DescriptionDefaults.kindOf(none));
    }
}
