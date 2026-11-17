//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.binding;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.interpret.ClassViews;
import org.opensilex.graphimport.interpret.InterpretFixtures;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.SO;

/**
 * What a decision about a sheet must respect, the columns left out included.
 *
 * @author Arnaud Charleroy
 */
public class SheetDecisionRulesTest {

    private static final SheetStructure SHEET = InterpretFixtures.sheet("Parcelles", List.of("nom", "remarque"),
            new String[]{"P1", "a"});
    private final ClassViews views = new ClassViews(InterpretFixtures::view);

    @Test
    public void aColumnMayBeLeftOut() throws Exception {
        assertTrue(whyNot(new ImportDecisions.SheetDecisions(SO, 0, Map.of(), Map.of(), Set.of(1))).isEmpty());
    }

    @Test
    public void aColumnTheSheetDoesNotHaveCannotBeLeftOut() throws Exception {
        assertEquals("The sheet has no column 3.",
                whyNot(new ImportDecisions.SheetDecisions(SO, 0, Map.of(), Map.of(), Set.of(2))).orElseThrow());
    }

    @Test
    public void aColumnCannotBeLeftOutAndGivenAPropertyAtOnce() throws Exception {
        assertTrue(whyNot(new ImportDecisions.SheetDecisions(SO, 0, Map.of(1, COMMENT), Map.of(), Set.of(1)))
                .orElseThrow().contains("left out"));
    }

    @Test
    public void aValueGivenForAllRowsNeedsTheClassOfTheSheet() throws Exception {
        assertTrue(whyNot(given(null, IS_PART_OF)).orElseThrow().contains("Choose the class"));
        assertTrue(whyNot(given(SO, IS_PART_OF)).isEmpty());
    }

    @Test
    public void aValueGivenForAllRowsIsOfAPropertyOfTheClassNoColumnGives() throws Exception {
        URI unknown = URI.create("http://test.opensilex.org/unknown");
        assertTrue(whyNot(given(SO, unknown)).orElseThrow().contains("does not accept"));
        assertTrue(whyNot(new ImportDecisions.SheetDecisions(SO, 0, Map.of(1, IS_PART_OF), Map.of(), Set.of(),
                Map.of(IS_PART_OF, "Bloc A"))).orElseThrow().contains("already gives"));
    }

    @Test
    public void aDeclaredResourceHasAClassANameAndPropertiesOfItsClass() throws Exception {
        assertTrue(whyNot(declared(SO, Map.of(NAME, List.of("Bloc A")))).isEmpty());
        assertTrue(whyNot(declared(SO, Map.of(COMMENT, List.of("nord")))).orElseThrow().contains("name"));
        assertTrue(whyNot(declared(URI.create("http://test.opensilex.org/Nothing"), Map.of(NAME, List.of("A"))))
                .orElseThrow().contains("cannot declare"));
        assertTrue(whyNot(declared(SO, Map.of(NAME, List.of("A"), COMMENT, List.of("x", "y")))).orElseThrow()
                .contains("cannot take 2 values"));
    }

    private static ImportDecisions.SheetDecisions given(URI classUri, URI property) {
        return new ImportDecisions.SheetDecisions(classUri, 0, Map.of(), Map.of(), Set.of(), Map.of(property, "Bloc A"));
    }

    private static ImportDecisions.DeclaredResource declared(URI classUri, Map<URI, List<String>> values) {
        return new ImportDecisions.DeclaredResource("r1", classUri, values);
    }

    private Optional<String> whyNot(ImportDecisions.DeclaredResource resource) throws Exception {
        return SheetDecisionRules.whyNot(resource, InterpretFixtures.CATALOG, views);
    }

    private Optional<String> whyNot(ImportDecisions.SheetDecisions decisions) throws Exception {
        return SheetDecisionRules.whyNot(decisions, SHEET, InterpretFixtures.CATALOG, views);
    }
}
