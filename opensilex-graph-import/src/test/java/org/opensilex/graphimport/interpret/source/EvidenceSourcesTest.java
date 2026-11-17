//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.SheetFacts;
import org.opensilex.graphimport.interpret.alias.LearnedAliases;
import org.opensilex.graphimport.ontology.HeaderResolver;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.graphimport.interpret.InterpretFixtures.COMMENT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.CREATION_DATE;
import static org.opensilex.graphimport.interpret.InterpretFixtures.GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.HAS_GERMPLASM;
import static org.opensilex.graphimport.interpret.InterpretFixtures.IS_PART_OF;
import static org.opensilex.graphimport.interpret.InterpretFixtures.NAME;
import static org.opensilex.graphimport.interpret.InterpretFixtures.PLOT;
import static org.opensilex.graphimport.interpret.InterpretFixtures.services;
import static org.opensilex.graphimport.interpret.InterpretFixtures.view;

/**
 * Each source alone, on a column of a sheet of plots.
 *
 * @author Arnaud Charleroy
 */
public class EvidenceSourcesTest {

    private static final HeaderResolver HEADERS = new HeaderResolver(Map.of());

    @Test
    public void aPredicateHeaderSettlesItsColumn() {
        HeaderSource source = new HeaderSource();

        assertEquals(Map.of(CREATION_DATE, 1.0), weights(source.gather(column("vocabulary:hasCreationDate"))));
        assertTrue(source.gather(column("vocabulary:hasNoSuchThing")).isEmpty());
        assertTrue(source.gather(column("rdf:type")).isEmpty());
        assertTrue(source.gather(column("Date de création")).isEmpty());
    }

    @Test
    public void anAliasCountsOnlyForAPropertyTheClassAccepts() {
        LearnedAliases aliases = (owner, classUri, header) -> "remarque".equals(header)
                ? Optional.of(COMMENT)
                : Optional.of(URI.create("http://test.opensilex.org/elsewhere"));
        InterpretationServices services = services((expected, names) -> Map.of(), aliases);
        LearnedAliasSource source = new LearnedAliasSource();

        assertEquals(Map.of(COMMENT, 0.8), weights(source.gather(column("Remarque", List.of(), false, List.of(), services))));
        assertTrue(source.gather(column("autre", List.of(), false, List.of(), services)).isEmpty());
    }

    @Test
    public void headersAreComparedWithLabelsLocalNamesAndCommonWords() {
        LabelSource source = new LabelSource();

        assertEquals(Map.of(NAME, 0.7), weights(source.gather(column("Nom"))));
        assertEquals(Map.of(CREATION_DATE, 0.7), weights(source.gather(column("date_de_creation"))));
        assertEquals(Map.of(IS_PART_OF, 0.7), weights(source.gather(column("isPartOf"))));
        double close = weights(source.gather(column("Date de creaton"))).get(CREATION_DATE);
        assertTrue(close > 0.4 && close < 0.5);
        assertTrue(source.gather(column("hauteur")).isEmpty());
    }

    @Test
    public void valuesConfirmOrDoubtALiteralProperty() {
        ValueTypeSource source = new ValueTypeSource();

        Map<URI, Double> dates = weights(source.gather(column("x", List.of("2026-04-03", "03/04/2026"))));
        assertEquals(0.1, dates.get(CREATION_DATE), 1e-9);
        assertEquals(0.1, dates.get(NAME), 1e-9);

        Map<URI, Double> words = weights(source.gather(column("x", List.of("abc", "def"))));
        assertEquals(-0.5, words.get(CREATION_DATE), 1e-9);

        Map<URI, Double> mixed = weights(source.gather(column("x", List.of("2026-04-03", "2026-04-04", "abc"))));
        assertEquals(-0.2, mixed.get(CREATION_DATE), 1e-9);
        assertTrue(source.gather(column("x", List.of())).isEmpty());
    }

    @Test
    public void valuesNamingVisibleInstancesProposeALink() throws Exception {
        InstanceIndex index = (expected, names) -> GERMPLASM.equals(expected)
                ? Map.of("cabernet", Set.of(URI.create("http://test/g/1")), "merlot", Set.of(URI.create("http://test/g/2")))
                : Map.of();
        InterpretationServices services = services(index, LearnedAliases.NONE);
        InstanceValueSource source = new InstanceValueSource();

        Map<URI, Double> weights = weights(source.gather(
                column("cépage", List.of("Cabernet", "Merlot", "Inconnu"), false, List.of(), services)));
        assertEquals(0.8 * 2 / 3, weights.get(HAS_GERMPLASM), 1e-9);
        assertEquals(1, weights.size());

        assertTrue(source.gather(column("cépage", List.of("Cabernet", "X", "Y"), false, List.of(), services)).isEmpty());
        assertTrue(source.gather(column("cépage", List.of("Cabernet"), true, List.of(), services)).isEmpty());
    }

    @Test
    public void identifiersOfASheetProposeALinkToItsRows() {
        List<SheetFacts> sheets = List.of(
                new SheetFacts("plots", view(PLOT), Set.of("p1", "p2")),
                new SheetFacts("varieties", view(GERMPLASM), Set.of("cabernet")),
                new SheetFacts("undecided", null, Set.of("p1")));
        SheetReferenceSource source = new SheetReferenceSource();

        assertEquals(Map.of(IS_PART_OF, 0.8),
                weights(source.gather(column("parent", List.of("P1", "p2"), false, sheets, services()))));
        assertEquals(Map.of(HAS_GERMPLASM, 0.8),
                weights(source.gather(column("cépage", List.of("Cabernet"), false, sheets, services()))));
        assertTrue(source.gather(column("nom", List.of("P1", "P2"), true, sheets, services())).isEmpty());
    }

    private static ColumnContext column(String header) {
        return column(header, List.of());
    }

    private static ColumnContext column(String header, List<String> values) {
        return column(header, values, false, List.of(), services());
    }

    private static ColumnContext column(String header, List<String> values, boolean identifier, List<SheetFacts> sheets,
                                        InterpretationServices services) {
        SheetStructure sheet = new SheetStructure().setName("plots").setHeaders(List.of(header)).setRows(List.of())
                .setTabular(true);
        return new ColumnContext(sheet, 0, header, HEADERS.read(header), values, view(PLOT), identifier, sheets,
                services);
    }

    private static Map<URI, Double> weights(List<Clue<URI>> clues) {
        return clues.stream().collect(Collectors.toMap(Clue::target, clue -> clue.evidence().weight(), Double::sum));
    }
}
