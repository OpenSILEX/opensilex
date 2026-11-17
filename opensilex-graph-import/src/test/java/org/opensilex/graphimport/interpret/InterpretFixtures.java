//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.interpret.alias.LearnedAliases;
import org.opensilex.graphimport.ontology.Cardinality;
import org.opensilex.graphimport.ontology.ClassHierarchy;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderResolver;
import org.opensilex.graphimport.ontology.Labels;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.RestrictionRow;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A small ontology without a triplestore: scientific objects with a plot subclass, and germplasm.
 *
 * @author Arnaud Charleroy
 */
public final class InterpretFixtures {

    public static final URI SO = URI.create(Oeso.ScientificObject.getURI());
    public static final URI PLOT = URI.create(Oeso.NS + "Plot");
    public static final URI GERMPLASM = URI.create(Oeso.Germplasm.getURI());
    public static final URI NAME = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
    public static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");
    public static final URI CREATION_DATE = URI.create(Oeso.NS + "hasCreationDate");
    public static final URI IS_PART_OF = URI.create(Oeso.NS + "isPartOf");
    public static final URI HAS_GERMPLASM = URI.create(Oeso.NS + "hasGermplasm");
    public static final URI OWNER = URI.create("http://test.opensilex.org/id/user/arnaud");

    private static final URI STRING = URI.create("http://www.w3.org/2001/XMLSchema#string");
    private static final URI DATE = URI.create("http://www.w3.org/2001/XMLSchema#date");

    public static final ClassHierarchy HIERARCHY = new ClassHierarchy().link(PLOT, SO);

    public static final ClassCatalog CATALOG = new ClassCatalog(Set.of(SO, GERMPLASM), Map.of(
            SO, new Labels(Map.of("en", "Scientific object", "fr", "Objet scientifique")),
            PLOT, new Labels(Map.of("en", "Plot", "fr", "Parcelle")),
            GERMPLASM, new Labels(Map.of("en", "Germplasm", "fr", "Matériel génétique"))), HIERARCHY);

    private InterpretFixtures() {
    }

    /** What a scientific object, and so a plot, accepts. */
    public static ClassView view(URI classUri) {
        if (GERMPLASM.equals(classUri)) {
            return ClassView.assemble(GERMPLASM, new ClassHierarchy(), List.of(
                    new RestrictionRow(GERMPLASM, NAME, PropertyKind.DATA, STRING, new Cardinality(1, 1))), Map.of());
        }
        return ClassView.assemble(classUri, HIERARCHY, List.of(
                new RestrictionRow(SO, NAME, PropertyKind.DATA, STRING, new Cardinality(1, 1)),
                new RestrictionRow(SO, COMMENT, PropertyKind.DATA, STRING, new Cardinality(0, 1)),
                new RestrictionRow(SO, CREATION_DATE, PropertyKind.DATA, DATE, new Cardinality(0, 1)),
                new RestrictionRow(SO, IS_PART_OF, PropertyKind.OBJECT, SO, new Cardinality(0, 1)),
                new RestrictionRow(SO, HAS_GERMPLASM, PropertyKind.OBJECT, GERMPLASM, Cardinality.ANY)),
                Map.of(CREATION_DATE, new Labels(Map.of("en", "Creation date", "fr", "Date de création")),
                        IS_PART_OF, new Labels(Map.of("en", "is part of", "fr", "fait partie de")),
                        HAS_GERMPLASM, new Labels(Map.of("en", "has germplasm"))));
    }

    public static InterpretationServices services(InstanceIndex instances, LearnedAliases aliases) {
        return new InterpretationServices(CATALOG, new ClassViews(InterpretFixtures::view), instances, aliases,
                new HeaderResolver(Map.of()), OWNER, "fr");
    }

    public static InterpretationServices services() {
        return services((expectedClass, names) -> Map.of(), LearnedAliases.NONE);
    }

    /** A sheet; each row is given as its cells, null for an empty one. */
    public static SheetStructure sheet(String name, List<String> headers, String[]... rows) {
        return new SheetStructure()
                .setName(name)
                .setHeaders(headers)
                .setRows(Arrays.stream(rows).map(Arrays::asList).toList())
                .setTabular(true);
    }
}
