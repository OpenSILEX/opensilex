//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The dictionary rows are ragged — four to seven cells for an eight-column header, with the empty
 * ones omitted rather than blanked — so the same index means a different field from one line to the
 * next. This is the test that protects the content-driven read; getting it wrong would silently
 * swap a unit for a type across the whole file.
 *
 * @author Arnaud Charleroy
 */
public class StarDictionaryTest {

    private static StarDictionary standard;
    private static StarDictionary example;

    @BeforeClass
    public static void readBothRevisions() throws Exception {
        standard = new StarDictionary(WorkbookFixture.starStandard());
        example = new StarDictionary(WorkbookFixture.starExample());
    }

    //#region the split revision

    @Test
    public void theSplitRevisionIsRead() {
        assertTrue(standard.isPresent());
        assertEquals("dictionary_variables holds ten measured columns",
                10, standard.getVariables().size());
        assertTrue("dictionary_metadata holds the structural ones",
                standard.getEntries().size() > 50);
    }

    @Test
    public void aSixCellVariableRowIsReadInFull() {
        // nom | description | unit | Rclass | uri | has_eloa_class
        StarDictionaryEntry sugar = standard.get("PM_BER_PC").orElseThrow(AssertionError::new);

        assertEquals("%", sugar.getUnit());
        assertEquals("numeric", sugar.getRClass());
        assertEquals("https://cropontology.org/term/CO_356:1000172", sugar.getUri());
        assertEquals("Variable", sugar.getEloaClass());
        assertTrue(standard.isVariable("PM_BER_PC"));
    }

    @Test
    public void aFourCellMetadataRowIsReadInFull() {
        // nom | description | Rclass | uri — no unit at all, and the cells shift left
        StarDictionaryEntry stage = standard.get("bbch_stage").orElseThrow(AssertionError::new);

        assertNull("a column with no unit must not borrow the next cell", stage.getUnit());
        assertEquals("character", stage.getRClass());
        assertEquals("vignevin:bbch_stage", stage.getUri());
        assertFalse(standard.isVariable("bbch_stage"));
    }

    @Test
    public void aFiveCellMetadataRowKeepsItsUnit() {
        // nom | description | unit | Rclass | uri
        StarDictionaryEntry spacing = standard.get("row_spacing").orElseThrow(AssertionError::new);

        assertEquals("m", spacing.getUnit());
        assertEquals("numeric", spacing.getRClass());
        assertEquals("vignevin:row_spacing", spacing.getUri());
    }

    //#endregion

    //#region the merged revision

    @Test
    public void theMergedRevisionIsReadToo() {
        assertTrue(example.isPresent());
        assertEquals("is_variable is what distinguishes them when both live in one sheet",
                11, example.getVariables().size());
    }

    @Test
    public void aSevenCellRowCarriesBothTheFlagAndTheClass() {
        // nom | description | unit | Rclass | uri | is_variable | has_eloa_class
        StarDictionaryEntry sugar = example.get("PM_BER_PC").orElseThrow(AssertionError::new);

        assertEquals("%", sugar.getUnit());
        assertEquals("numeric", sugar.getRClass());
        assertEquals("https://cropontology.org/term/CO_356:1000172", sugar.getUri());
        assertEquals(Boolean.TRUE, sugar.getVariable());
        assertEquals("Variable", sugar.getEloaClass());
    }

    @Test
    public void aFiveCellRowIsNotMistakenForOneWithAUnit() {
        // nom | description | Rclass | uri | is_variable — read by index, 'character' would be
        // taken for a unit and the URI for a type.
        StarDictionaryEntry stage = example.get("bbch_stage").orElseThrow(AssertionError::new);

        assertNull(stage.getUnit());
        assertEquals("character", stage.getRClass());
        assertEquals("vignevin:bbch_stage", stage.getUri());
        assertEquals(Boolean.FALSE, stage.getVariable());
    }

    @Test
    public void aSixCellRowEndingWithAnEloaClassIsRead() {
        // nom | description | Rclass | uri | is_variable | has_eloa_class
        StarDictionaryEntry treatment = example.get("xp_trt_code").orElseThrow(AssertionError::new);

        assertNull(treatment.getUnit());
        assertEquals("character", treatment.getRClass());
        assertEquals(Boolean.FALSE, treatment.getVariable());
        assertEquals("Traitement expérimental", treatment.getEloaClass());
    }

    //#endregion

    //#region both revisions agree

    @Test
    public void theTwoRevisionsDisagreeAboutRainfall() {
        // rain_mm sits in dictionary_variables in neither: the reference template files it under
        // metadata, beside plot_id and commune_name, while the example flags it as a variable.
        // It is a measured quantity and a column of data_meteo, so the template is the one at
        // fault — and an import driven by it would quietly drop rainfall.
        assertFalse("the reference template does not declare rainfall as a variable",
                standard.isVariable("rain_mm"));
        assertTrue("the example does", example.isVariable("rain_mm"));

        assertEquals("it is described either way, so the disagreement is about its role only",
                "mm", standard.get("rain_mm").orElseThrow(AssertionError::new).getUnit());
    }

    @Test
    public void bothRevisionsDescribeTheSameVariables() {
        for (StarDictionaryEntry entry : standard.getVariables()) {
            StarDictionaryEntry other = example.get(entry.getName()).orElse(null);
            assertTrue("the example should describe " + entry.getName(), other != null);
            assertEquals(entry.getName(), entry.getUnit(), other.getUnit());
            assertEquals(entry.getName(), entry.getRClass(), other.getRClass());
            assertEquals(entry.getName(), entry.getUri(), other.getUri());
        }
    }

    @Test
    public void theMisspeltIntegerTypeIsAcceptedAndMentioned() {
        assertEquals("interger", standard.get("plot_n").orElseThrow(AssertionError::new).getRClass());
        assertEquals(XSDDatatype.XSDinteger.getURI(), StarDictionary.toDatatype("interger"));
        assertTrue("the typo is worth one remark, not silence",
                standard.getNotes().stream().anyMatch(note -> note.contains("interger")));
    }

    @Test
    public void rClassesMapOntoTheDatatypesOpenSilexOffers() {
        assertEquals(XSDDatatype.XSDdecimal.getURI(), StarDictionary.toDatatype("numeric"));
        assertEquals(XSDDatatype.XSDstring.getURI(), StarDictionary.toDatatype("character"));
        assertEquals(XSDDatatype.XSDdate.getURI(), StarDictionary.toDatatype("date"));
        assertNull(StarDictionary.toDatatype("something else"));
        assertNull(StarDictionary.toDatatype(null));
    }

    //#endregion
}
