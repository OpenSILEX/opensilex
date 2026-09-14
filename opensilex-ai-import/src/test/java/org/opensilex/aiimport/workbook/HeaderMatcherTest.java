//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Arnaud Charleroy
 */
public class HeaderMatcherTest {

    /**
     * The real header row of the VitisExplorer catalogue sheet, version suffixes included.
     */
    private static final List<String> HEADERS = Arrays.asList(
            "Catégorie/Onglet", "Libellé", "Table_CropOntology_20220616", "Nom de la variable",
            "Nouvelle_Abréviation_FR_maj_03_2023", "Hiver");

    @Test
    public void accentsAndSeparatorsAreIgnored() {
        assertEquals("Nouvelle_Abréviation_FR_maj_03_2023", HeaderMatcher.find(HEADERS, "abreviation"));
        assertEquals("Libellé", HeaderMatcher.find(HEADERS, "libelle"));
    }

    @Test
    public void aVersionSuffixDoesNotPreventAMatch() {
        assertEquals("Table_CropOntology_20220616", HeaderMatcher.find(HEADERS, "cropontology"));
    }

    @Test
    public void allWordsMustBePresent() {
        assertEquals("Nom de la variable", HeaderMatcher.find(HEADERS, "nom", "variable"));
        assertNull(HeaderMatcher.find(HEADERS, "nom", "unite"));
    }

    @Test
    public void anAbsentColumnReturnsNull() {
        assertNull(HeaderMatcher.find(HEADERS, "unite"));
    }
}
