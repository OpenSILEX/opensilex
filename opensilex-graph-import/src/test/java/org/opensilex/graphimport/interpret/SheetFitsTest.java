//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.junit.Test;
import org.opensilex.graphimport.ontology.Cardinality;
import org.opensilex.graphimport.ontology.ClassHierarchy;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.RestrictionRow;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.opensilex.graphimport.interpret.InterpretFixtures.GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.HAS_GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.SO;

/**
 * Which sheets a link property may point to, by the classes of their rows.
 *
 * @author Arnaud Charleroy
 */
public class SheetFitsTest {

    /** A class above the roots the assistant imports: no screen knows it, the server does. */
    private static final URI THING = URI.create("http://test.opensilex.org/ontology#Thing");
    private static final URI LOCATED_IN = URI.create("http://test.opensilex.org/ontology#locatedIn");
    private static final ClassHierarchy HIERARCHY = new ClassHierarchy().link(PLOT, SO).link(SO, THING);

    private final ClassViews views = new ClassViews(SheetFitsTest::view);

    @Test
    public void aLinkReachesTheSheetsWhoseRowsItsRangeAccepts() throws Exception {
        Interpretation interpretation = new Interpretation(List.of(
                sheet("Parcelles", PLOT, Set.of()),
                sheet("Objets", SO, Set.of()),
                sheet("Matériel", GERMPLASM, Set.of()),
                sheet("Sans classe", null, Set.of())), List.of());

        Map<URI, List<String>> fromPlots = SheetFits.of(interpretation, views).get("Parcelles");

        assertEquals("a subclass of the range fits; another class does not", List.of("Parcelles", "Objets"),
                fromPlots.get(IS_PART_OF));
        assertEquals(List.of("Matériel"), fromPlots.get(HAS_GERMPLASM));
        assertEquals("a range above the roots accepts what descends from it", List.of("Parcelles", "Objets"),
                fromPlots.get(LOCATED_IN));
    }

    @Test
    public void theClassesATypeColumnGivesCountToo() throws Exception {
        Interpretation interpretation = new Interpretation(List.of(
                sheet("Parcelles", PLOT, Set.of()),
                sheet("Mélange", GERMPLASM, Set.of(PLOT))), List.of());

        assertEquals(List.of("Parcelles", "Mélange"),
                SheetFits.of(interpretation, views).get("Parcelles").get(IS_PART_OF));
    }

    private static SheetInterpretation sheet(String name, URI classUri, Set<URI> rowClasses) {
        Choice<URI> sheetClass = classUri == null
                ? Choice.undecided(List.of(), List.of())
                : new Choice<>(classUri, Choice.Status.CHOSEN, List.of(), List.of());
        return new SheetInterpretation(name, 1, sheetClass, rowClasses, Choice.undecided(List.of(), List.of()),
                List.of(), classUri == null ? null : view(classUri), List.of());
    }

    private static ClassView view(URI classUri) {
        if (GERMPLASM.equals(classUri)) {
            return InterpretFixtures.view(GERMPLASM);
        }
        return ClassView.assemble(classUri, HIERARCHY, List.of(
                new RestrictionRow(SO, IS_PART_OF, PropertyKind.OBJECT, SO, Cardinality.ANY),
                new RestrictionRow(SO, HAS_GERMPLASM, PropertyKind.OBJECT, GERMPLASM, Cardinality.ANY),
                new RestrictionRow(SO, LOCATED_IN, PropertyKind.OBJECT, THING, Cardinality.ANY)), Map.of());
    }
}
