//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The mapping that has to work when nothing else does.
 * <p>
 * No template recognised, no language model reachable: a header dictionary is all that is left to
 * tell the user what their columns are. These cases come from the three workbook families the
 * module knows, in both languages, plus the near-misses that a looser matcher would get wrong.
 *
 * @author Arnaud Charleroy
 */
public class HeaderRoleDictionaryTest {

    @Test
    public void theObservedObjectIsRecognisedAcrossTemplates() {
        assertEquals(ColumnRole.OBJECT, HeaderRoleDictionary.roleOf("plot_id"));
        assertEquals(ColumnRole.OBJECT, HeaderRoleDictionary.roleOf("Placette"));
        assertEquals(ColumnRole.OBJECT, HeaderRoleDictionary.roleOf("Observation unit ID"));
        assertEquals(ColumnRole.OBJECT, HeaderRoleDictionary.roleOf("PU"));
    }

    @Test
    public void theTrialAndTheProjectAreToldApart() {
        assertEquals(ColumnRole.TRIAL, HeaderRoleDictionary.roleOf("expe_id"));
        assertEquals(ColumnRole.TRIAL, HeaderRoleDictionary.roleOf("Dispositif"));
        assertEquals(ColumnRole.TRIAL, HeaderRoleDictionary.roleOf("Study unique ID"));
        assertEquals(ColumnRole.PROJECT, HeaderRoleDictionary.roleOf("proj_id"));
        assertEquals(ColumnRole.PROJECT, HeaderRoleDictionary.roleOf("Investigation title"));
    }

    @Test
    public void aPersonIsNotAnObserver() {
        assertEquals(ColumnRole.PERSON, HeaderRoleDictionary.roleOf("Person name"));
        assertEquals(ColumnRole.PERSON, HeaderRoleDictionary.roleOf("orcid"));
        assertEquals(ColumnRole.OBSERVER, HeaderRoleDictionary.roleOf("Observateur"));
        assertEquals(ColumnRole.OBSERVER, HeaderRoleDictionary.roleOf("p_operator"));
    }

    @Test
    public void accentsAndSeparatorsDoNotMatter() {
        assertEquals(ColumnRole.GERMPLASM, HeaderRoleDictionary.roleOf("Variété"));
        assertEquals(ColumnRole.GERMPLASM, HeaderRoleDictionary.roleOf("cultivar_name"));
        assertEquals(ColumnRole.FACTOR_LEVEL, HeaderRoleDictionary.roleOf("Modalité"));
        assertEquals(ColumnRole.SEASON, HeaderRoleDictionary.roleOf("Année"));
    }

    @Test
    public void aDateIsRecognisedByTheParserThatWillReadIt() {
        assertEquals(ColumnRole.DATE, HeaderRoleDictionary.roleOf("observation_date"));
        assertEquals(ColumnRole.DATE, HeaderRoleDictionary.roleOf("meteo_datetime"));
    }

    /**
     * A property of the object is not a measurement of it, and the difference decides where the
     * value is written: once on the scientific object, or once per observation with a date.
     */
    @Test
    public void aPropertyOfTheObjectIsNotAMeasurement() {
        assertEquals(ColumnRole.OBJECT_PROPERTY, HeaderRoleDictionary.roleOf("Premier_Rang"));
        assertEquals(ColumnRole.OBJECT_PROPERTY, HeaderRoleDictionary.roleOf("Derniere_Souche"));
        assertEquals(ColumnRole.OBJECT_PROPERTY, HeaderRoleDictionary.roleOf("row_spacing"));
        assertEquals(ColumnRole.OBJECT_PROPERTY, HeaderRoleDictionary.roleOf("Écartement"));
    }

    /**
     * A coordinate is a dated move event in OpenSILEX, not an attribute of the object — its
     * scientific object CSV import carries exactly these columns and turns them into a move.
     */
    @Test
    public void aCoordinateIsAPositionRatherThanAProperty() {
        assertEquals(ColumnRole.POSITION, HeaderRoleDictionary.roleOf("plot_x"));
        assertEquals(ColumnRole.POSITION, HeaderRoleDictionary.roleOf("plot_y"));
        assertEquals(ColumnRole.POSITION, HeaderRoleDictionary.roleOf("x"));
        assertEquals(ColumnRole.POSITION, HeaderRoleDictionary.roleOf("textualPosition"));
    }

    /**
     * A block groups plots; it is not a treatment. Two plots in the same block can carry different
     * treatments — that is what blocking is for.
     */
    @Test
    public void aBlockIsNotATreatment() {
        assertEquals(ColumnRole.PARENT_OBJECT, HeaderRoleDictionary.roleOf("block_code"));
        assertEquals(ColumnRole.PARENT_OBJECT, HeaderRoleDictionary.roleOf("Bloc"));
        assertEquals(ColumnRole.FACTOR_LEVEL, HeaderRoleDictionary.roleOf("xp_trt_code"));
        assertEquals(ColumnRole.FACTOR_LEVEL, HeaderRoleDictionary.roleOf("Modalité"));
    }

    /**
     * The mapping's own limits, asserted so a later widening of the table has to face them.
     */
    @Test
    public void aColumnMeasuringSomethingIsNotTheThingItMeasures() {
        // 'bloc' appears in the middle, and the column holds an area, not a block identifier.
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("surface_bloc_m2"));
        // A measurement whose name contains no known term at all.
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("PM_LEAF_PC"));
    }

    /**
     * A word that means two things in two templates is left undecided rather than guessed: STAR
     * calls a field a 'parcelle' and treats it as a facility, other templates use it for the plot.
     */
    @Test
    public void anAmbiguousWordIsLeftToTheProfileThatKnows() {
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("parcelle"));
        // Too generic to decide on, in any template.
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("code"));
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("name"));
    }

    @Test
    public void anEmptyHeaderIsUnknownRatherThanAnything() {
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf(null));
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("   "));
        assertEquals(ColumnRole.UNKNOWN, HeaderRoleDictionary.roleOf("---"));
    }
}
