//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.ontology.HeaderReading;
import org.opensilex.graphimport.ontology.HeaderResolver;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.interpret.InterpretFixtures.sheet;

/**
 * The identifier column found before the class of the sheet is known.
 *
 * @author Arnaud Charleroy
 */
public class IdentifierDetectorTest {

    private static final HeaderResolver HEADERS = new HeaderResolver(Map.of());

    @Test
    public void aNamedColumnOfUniqueValuesIdentifiesTheRows() {
        SheetStructure plots = sheet("plots", List.of("hauteur", "nom", "bloc"),
                new String[]{"1,5", "P1", "B1"},
                new String[]{"2,5", "P2", "B1"},
                new String[]{"3,5", "P3", "B2"});

        Choice<Integer> identifier = detect(plots);

        assertEquals(Integer.valueOf(1), identifier.chosen());
        assertEquals(Choice.Status.SUGGESTED, identifier.status());
        assertEquals(0.7, identifier.candidates().get(0).score(), 1e-9);
    }

    @Test
    public void repeatedValuesAreAllowedButCountLess() {
        SheetStructure notes = sheet("notes", List.of("id", "commentaire"),
                new String[]{"P1", "a"},
                new String[]{"P1", "b"},
                new String[]{"P2", "c"});

        Choice<Integer> identifier = detect(notes);

        assertEquals(Integer.valueOf(0), identifier.candidates().get(0).target());
        assertEquals(0.5, identifier.candidates().get(0).score(), 1e-9);
        assertEquals(Choice.Status.UNDECIDED, identifier.status());
    }

    @Test
    public void twoLikelyColumnsAreLeftToTheUser() {
        SheetStructure plots = sheet("plots", List.of("rdfs:label", "uri"),
                new String[]{"P1", "http://opensilex.test/id/p1"},
                new String[]{"P2", null});

        Choice<Integer> identifier = detect(plots);

        assertEquals(Choice.Status.UNDECIDED, identifier.status());
        assertEquals(Integer.valueOf(0), identifier.candidates().get(0).target());
        assertEquals(0.4 + 0.3 + 0.1, identifier.candidates().get(0).score(), 1e-9);
        assertEquals(0.4 + 0.2 + 0.15, identifier.candidates().get(1).score(), 1e-9);
    }

    @Test
    public void measuresTypesAndEmptyColumnsAreLeftOut() {
        SheetStructure measures = sheet("measures", List.of("hauteur", "a", "date", "vide"),
                new String[]{"1,5", "vocabulary:Plot", "2026-04-03", null},
                new String[]{"2,5", "vocabulary:Plot", "2026-04-04", "NA"});

        Choice<Integer> identifier = detect(measures);

        assertNull(identifier.chosen());
        assertTrue(identifier.candidates().isEmpty());
    }

    private static Choice<Integer> detect(SheetStructure sheet) {
        List<HeaderReading> readings = sheet.getHeaders().stream().map(HEADERS::read).toList();
        return IdentifierDetector.detect(sheet, readings);
    }
}
