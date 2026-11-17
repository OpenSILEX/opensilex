//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.rml;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateFixtures;
import org.opensilex.aiimport.template.TemplateSheet;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * What the RML says: a triples map per sheet of objects with its class and one predicate per column,
 * a map per measured variable that states an observation in SOSA, and the join between the two.
 *
 * @author Arnaud Charleroy
 */
public class RmlWriterTest {

    private static final IRI TRIPLES_MAP = SimpleValueFactory.getInstance().createIRI(RmlWriter.RR + "TriplesMap");

    private static IRI iri(String uri) {
        return SimpleValueFactory.getInstance().createIRI(uri);
    }

    private static Model read(String turtle) throws Exception {
        return Rio.parse(new StringReader(turtle), "", RDFFormat.TURTLE);
    }

    private static Set<String> predicatesOf(Model model, Resource map) {
        return model.filter(map, iri(RmlWriter.RR + "predicateObjectMap"), null).objects().stream()
                .flatMap(entry -> model.filter((Resource) entry, iri(RmlWriter.RR + "predicate"), null).objects()
                        .stream()).map(Value::stringValue).collect(Collectors.toSet());
    }

    @Test
    public void theDocumentIsTurtleThatReadsBackAndSaysItsSubjectsAreIndicative() throws Exception {
        String turtle = new RmlWriter().write(TemplateFixtures.planAndMeasures());

        assertTrue(turtle.startsWith("# The IRI templates"));
        assertFalse(read(turtle).isEmpty());
    }

    @Test
    public void aSheetOfObjectsIsATriplesMapWithItsClassAndAPredicatePerColumn() throws Exception {
        Model model = read(new RmlWriter().write(TemplateFixtures.planAndMeasures()));

        Resource plan = model.filter(null, RDF.TYPE, TRIPLES_MAP).subjects().stream()
                .filter(map -> map.stringValue().endsWith("/map/plan")).findFirst().orElseThrow();
        Resource subject = (Resource) model.filter(plan, iri(RmlWriter.RR + "subjectMap"), null).objects()
                .iterator().next();
        assertEquals(TemplateFixtures.PLOT.toString(),
                model.filter(subject, iri(RmlWriter.RR + "class"), null).objects().iterator().next().stringValue());
        assertTrue(model.filter(subject, iri(RmlWriter.RR + "template"), null).objects().iterator().next()
                .stringValue().contains("{plot_id}"));
        Set<String> predicates = predicatesOf(model, plan);
        assertTrue(predicates.contains("http://www.w3.org/2000/01/rdf-schema#label"));
        assertTrue(predicates.contains("http://www.opensilex.org/vocabulary/oeso#hasGermplasm"));
        assertTrue(predicates.contains("http://www.opensilex.org/vocabulary/oeso#hasFactorLevel"));
        assertTrue("the property the user chose", predicates.contains(TemplateFixtures.ALTITUDE));
        Resource source = (Resource) model.filter(plan, iri(RmlWriter.RML + "logicalSource"), null).objects()
                .iterator().next();
        assertEquals("plan.csv", model.filter(source, iri(RmlWriter.RML + "source"), null).objects().iterator()
                .next().stringValue());
        assertEquals(RmlWriter.QL + "CSV", model.filter(source, iri(RmlWriter.RML + "referenceFormulation"), null)
                .objects().iterator().next().stringValue());
    }

    @Test
    public void aMeasuredVariableIsAnObservationJoinedToItsObjects() throws Exception {
        Model model = read(new RmlWriter().write(TemplateFixtures.planAndMeasures()));

        Resource observation = model.filter(null, RDF.TYPE, TRIPLES_MAP).subjects().stream()
                .filter(map -> map.stringValue().endsWith("/map/mesures/hauteur_cm")).findFirst().orElseThrow();
        Set<String> predicates = predicatesOf(model, observation);
        assertTrue(predicates.contains(RmlWriter.SOSA + "hasSimpleResult"));
        assertTrue(predicates.contains(RmlWriter.SOSA + "observedProperty"));
        assertTrue(predicates.contains(RmlWriter.SOSA + "hasFeatureOfInterest"));
        assertFalse("a variable is not a property of the objects",
                predicatesOf(model, model.filter(null, RDF.TYPE, TRIPLES_MAP).subjects().stream()
                        .filter(map -> map.stringValue().endsWith("/map/plan")).findFirst().orElseThrow())
                        .contains(RmlWriter.SOSA + "observedProperty"));
        assertEquals("the join is on the name of the object", 1,
                model.filter(null, iri(RmlWriter.RR + "joinCondition"), null).size());
        assertTrue(model.filter(null, iri(RmlWriter.RR + "parentTriplesMap"), null).objects().stream()
                .anyMatch(map -> map.stringValue().endsWith("/map/plan")));
    }

    @Test
    public void objectsAndObservationsInOneSheetNeedNoJoin() throws Exception {
        FileTemplate flat = FileTemplate.fromSheets("plat", List.of(new TemplateSheet("essai",
                List.of("plot_id", "date", "hauteur_cm"), SheetKind.OBJECTS_AND_DATA,
                Map.of("plot_id", DescribedRole.OBJECT, "date", DescribedRole.DATE,
                        "hauteur_cm", DescribedRole.VARIABLE), null, Map.of())));

        Model model = read(new RmlWriter().write(flat));

        assertTrue(model.filter(null, iri(RmlWriter.RR + "joinCondition"), null).isEmpty());
        assertEquals(1, model.filter(null, iri(RmlWriter.RR + "parentTriplesMap"), null).size());
    }

    @Test
    public void aRoleWithoutAnOntologyLinkProducesNoTriple() throws Exception {
        FileTemplate commented = FileTemplate.fromSheets("note", List.of(new TemplateSheet("plan",
                List.of("plot_id", "remarque"), SheetKind.OBJECTS,
                Map.of("plot_id", DescribedRole.OBJECT, "remarque", DescribedRole.COMMENT), null, Map.of())));

        Model model = read(new RmlWriter().write(commented));

        Resource plan = model.filter(null, RDF.TYPE, TRIPLES_MAP).subjects().iterator().next();
        assertEquals(Set.of("http://www.w3.org/2000/01/rdf-schema#label"), predicatesOf(model, plan));
        assertFalse(model.toString().contains("remarque"));
    }

    @Test
    public void aSheetIgnoredOrWithoutObjectsWritesNothing() throws Exception {
        FileTemplate template = FileTemplate.fromSheets("rien", List.of(new TemplateSheet("lisezmoi",
                List.of("a", "b"), SheetKind.IGNORED, Map.of(), null, Map.of())));

        assertTrue(read(new RmlWriter().write(template)).isEmpty());
    }
}
