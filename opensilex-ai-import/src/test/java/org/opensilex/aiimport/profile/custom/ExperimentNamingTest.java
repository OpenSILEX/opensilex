//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * Which sheets need an experiment, and which name it themselves.
 *
 * @author Arnaud Charleroy
 */
public class ExperimentNamingTest {

    @Test
    public void objectsObservationsAndLevelsBelongToAnExperiment() {
        assertTrue(ExperimentNaming.needs(SheetKind.OBJECTS));
        assertTrue(ExperimentNaming.needs(SheetKind.DATA));
        assertTrue(ExperimentNaming.needs(SheetKind.OBJECTS_AND_DATA));
        assertTrue(ExperimentNaming.needs(SheetKind.FACTORS));
        assertFalse(ExperimentNaming.needs(SheetKind.FACILITIES));
        assertFalse("a sheet of experiments names one, it does not need one", ExperimentNaming.needs(SheetKind.EXPERIMENTS));
        assertFalse(ExperimentNaming.needs(SheetKind.IGNORED));
    }

    @Test
    public void aColumnWithANameInItNamesTheExperiment() {
        DescribedSheet named = DescribedSheet.of(sheet("plan", Arrays.asList("plot_id", "essai"),
                row("P01", "Essai A")), description("essai"));
        DescribedSheet empty = DescribedSheet.of(sheet("plan", Arrays.asList("plot_id", "essai"),
                row("P01", "")), description("essai"));
        DescribedSheet none = DescribedSheet.of(sheet("plan", Arrays.asList("plot_id", "variete"),
                row("P01", "Syrah")), null);

        assertTrue(ExperimentNaming.names(named));
        assertFalse("an empty column names nothing", ExperimentNaming.names(empty));
        assertFalse(ExperimentNaming.names(none));
    }

    private static SheetDescription description(String trialColumn) {
        SheetDescription description = new SheetDescription("plan");
        description.getRoles().put(trialColumn, DescribedRole.TRIAL);
        return description;
    }
}
