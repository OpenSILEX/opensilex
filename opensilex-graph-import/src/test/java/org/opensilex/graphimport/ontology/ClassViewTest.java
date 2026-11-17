//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * How the restrictions of a class and of its ancestors combine, without a triplestore.
 *
 * @author Arnaud Charleroy
 */
public class ClassViewTest {

    private static final String NS = "http://test.opensilex.org/graph-import#";
    private static final URI THING = URI.create(NS + "Thing");
    private static final URI PLANT = URI.create(NS + "Plant");
    private static final URI TREE = URI.create(NS + "Tree");
    private static final URI LABELLED = URI.create(NS + "Labelled");
    private static final URI HEIGHT = URI.create(NS + "height");
    private static final URI NOTE = URI.create(NS + "note");
    private static final URI DECIMAL = URI.create("http://www.w3.org/2001/XMLSchema#decimal");
    private static final URI STRING = URI.create("http://www.w3.org/2001/XMLSchema#string");

    private final ClassHierarchy hierarchy = new ClassHierarchy()
            .link(TREE, PLANT)
            .link(PLANT, THING)
            .link(TREE, LABELLED);

    @Test
    public void theNearestClassWins() {
        ClassView tree = ClassView.assemble(TREE, hierarchy, List.of(
                row(THING, HEIGHT, Cardinality.of(null, null, 1)),
                row(TREE, HEIGHT, Cardinality.of(null, null, 3))), Map.of());

        PropertyView height = tree.getProperty(HEIGHT);
        assertEquals(new Cardinality(0, 3), height.cardinality());
        assertEquals(Set.of(TREE), height.declaredOn());
    }

    @Test
    public void theNearestClassWinsEvenWhenTheTriplestoreLinksTheGrandparentDirectly() {
        ClassHierarchy inferred = new ClassHierarchy().link(TREE, PLANT).link(PLANT, THING).link(TREE, THING);

        ClassView tree = ClassView.assemble(TREE, inferred, List.of(
                row(THING, HEIGHT, Cardinality.of(null, null, 1)),
                row(PLANT, HEIGHT, Cardinality.of(null, null, 3))), Map.of());

        assertEquals(new Cardinality(0, 3), tree.getProperty(HEIGHT).cardinality());
    }

    @Test
    public void restrictionsOfTheSameRankAddUp() {
        ClassView tree = ClassView.assemble(TREE, hierarchy, List.of(
                row(PLANT, NOTE, Cardinality.of(null, 1, null)),
                row(LABELLED, NOTE, Cardinality.of(null, null, 2)),
                row(TREE, HEIGHT, Cardinality.of(null, 1, null)),
                row(TREE, HEIGHT, Cardinality.of(null, null, 1))), Map.of());

        assertEquals(new Cardinality(1, 2), tree.getProperty(NOTE).cardinality());
        assertEquals(Set.of(PLANT, LABELLED), tree.getProperty(NOTE).declaredOn());
        assertEquals(new Cardinality(1, 1), tree.getProperty(HEIGHT).cardinality());
    }

    @Test
    public void aPropertyOfAnotherBranchIsNotAccepted() {
        URI rock = URI.create(NS + "Rock");
        ClassView tree = ClassView.assemble(TREE, hierarchy.link(rock, THING), List.of(
                row(rock, HEIGHT, Cardinality.ANY)), Map.of());

        assertNull(tree.getProperty(HEIGHT));
        assertTrue(tree.getProperties().isEmpty());
        assertTrue(tree.isA(THING));
        assertFalse(tree.isA(rock));
    }

    @Test
    public void aPropertyNothingTellsTheKindOfIsLeftOut() {
        ClassView tree = ClassView.assemble(TREE, hierarchy, List.of(
                new RestrictionRow(TREE, NOTE, null, null, Cardinality.ANY)), Map.of());

        assertNull(tree.getProperty(NOTE));
    }

    @Test
    public void labelsFallBackToTheUntaggedOneThenToEnglish() {
        Map<URI, Labels> labels = Map.of(
                HEIGHT, new Labels(Map.of("en", "height", "fr", "hauteur")),
                TREE, new Labels(Map.of("", "Arbre")));

        ClassView tree = ClassView.assemble(TREE, hierarchy, List.of(row(TREE, HEIGHT, Cardinality.ANY)), labels);

        assertEquals("hauteur", tree.getProperty(HEIGHT).labels().in("fr"));
        assertEquals("height", tree.getProperty(HEIGHT).labels().in("de"));
        assertEquals("Arbre", tree.getLabels().in("en"));
        assertNull(Labels.NONE.in("fr"));
    }

    @Test
    public void twoRangesKeepTheOneOfTheFirstClassInUriOrder() {
        ClassView tree = ClassView.assemble(TREE, hierarchy, List.of(
                new RestrictionRow(PLANT, NOTE, PropertyKind.DATA, STRING, Cardinality.ANY),
                new RestrictionRow(LABELLED, NOTE, PropertyKind.DATA, DECIMAL, Cardinality.ANY)), Map.of());

        assertEquals(DECIMAL, tree.getProperty(NOTE).range());
    }

    private static RestrictionRow row(URI domain, URI property, Cardinality cardinality) {
        return new RestrictionRow(domain, property, PropertyKind.DATA, DECIMAL, cardinality);
    }
}
