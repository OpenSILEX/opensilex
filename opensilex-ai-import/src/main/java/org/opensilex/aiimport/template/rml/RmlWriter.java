//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.rml;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.RoleOntology;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateSheet;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Writes a template as RML, the mapping language that says how the cells of a CSV file become RDF:
 * a triples map per sheet of objects, with the class of the objects and a predicate per column, and
 * a map per measured variable that states an observation in SOSA.
 * <p>
 * Write-only, and nothing in the module runs it: it is there so that what a user taught the module
 * can be taken to a tool that does. The IRI templates of the subjects are indicative — the platform
 * makes the real URIs when it creates an object — and the document says so.
 *
 * @author Arnaud Charleroy
 */
public final class RmlWriter {

    static final String RR = "http://www.w3.org/ns/r2rml#";
    static final String RML = "http://semweb.mmlab.be/ns/rml#";
    static final String QL = "http://semweb.mmlab.be/ns/ql#";
    static final String SOSA = "http://www.w3.org/ns/sosa/";

    private static final String NOTE = "# The IRI templates of the subjects below are indicative: OpenSILEX makes the\n"
            + "# real URIs when it creates an object. Written by the import assistant; it is not run by it.\n";

    private static final String INDICATIVE_BASE = "http://opensilex.org/id/";

    private final ValueFactory values = SimpleValueFactory.getInstance();

    /**
     * @return the template as RML in Turtle
     */
    public String write(FileTemplate template) {
        Model model = new LinkedHashModel();
        model.setNamespace("rr", RR);
        model.setNamespace("rml", RML);
        model.setNamespace("ql", QL);
        model.setNamespace("sosa", SOSA);
        model.setNamespace("rdfs", RDFS.NAMESPACE);

        Map<String, Resource> objectMaps = new LinkedHashMap<>();
        Map<String, String> nameColumns = new LinkedHashMap<>();
        for (TemplateSheet sheet : template.getSheets()) {
            Optional<String> nameColumn = column(sheet, DescribedRole.OBJECT);
            if (nameColumn.isPresent() && describesObjects(sheet)) {
                objectMaps.put(sheet.getName(), objectMap(model, sheet, nameColumn.get()));
                nameColumns.put(sheet.getName(), nameColumn.get());
            }
        }
        for (TemplateSheet sheet : template.getSheets()) {
            Optional<String> nameColumn = column(sheet, DescribedRole.OBJECT);
            if (nameColumn.isPresent() && holdsData(sheet)) {
                observationMaps(model, sheet, nameColumn.get(), objectMaps, nameColumns);
            }
        }

        StringWriter out = new StringWriter();
        out.write(NOTE);
        Rio.write(model, out, RDFFormat.TURTLE);
        return out.toString();
    }

    private Resource objectMap(Model model, TemplateSheet sheet, String nameColumn) {
        IRI map = values.createIRI(INDICATIVE_BASE + "map/" + token(sheet.getName()));
        model.add(map, RDF.TYPE, values.createIRI(RR + "TriplesMap"));
        source(model, map, sheet);

        BNode subject = values.createBNode();
        model.add(map, values.createIRI(RR + "subjectMap"), subject);
        model.add(subject, values.createIRI(RR + "template"),
                values.createLiteral(INDICATIVE_BASE + "object/{" + nameColumn + "}"));
        if (sheet.getType() != null) {
            model.add(subject, values.createIRI(RR + "class"), values.createIRI(sheet.getType().toString()));
        }

        Map<String, DescribedRole> roles = sheet.roles();
        for (String header : sheet.getHeaders()) {
            DescribedRole role = roles.get(header);
            String property = sheet.getProperties().get(header);
            String predicate = property != null && !property.isBlank()
                    ? property
                    : role == null || role == DescribedRole.VARIABLE ? null : RoleOntology.predicate(role).orElse(null);
            if (predicate != null) {
                reference(model, map, predicate, header);
            }
        }
        return map;
    }

    private void observationMaps(Model model, TemplateSheet sheet, String nameColumn, Map<String, Resource> objectMaps,
                                 Map<String, String> nameColumns) {
        Map<String, DescribedRole> roles = sheet.roles();
        Optional<String> dateColumn = column(sheet, DescribedRole.DATE);
        Optional<Map.Entry<String, Resource>> parent = objectMaps.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(sheet.getName())).findFirst();
        for (String header : sheet.getHeaders()) {
            if (roles.get(header) != DescribedRole.VARIABLE) {
                continue;
            }
            IRI map = values.createIRI(INDICATIVE_BASE + "map/" + token(sheet.getName()) + "/" + token(header));
            model.add(map, RDF.TYPE, values.createIRI(RR + "TriplesMap"));
            source(model, map, sheet);

            BNode subject = values.createBNode();
            model.add(map, values.createIRI(RR + "subjectMap"), subject);
            model.add(subject, values.createIRI(RR + "template"), values.createLiteral(INDICATIVE_BASE
                    + "observation/{" + nameColumn + "}" + dateColumn.map(date -> "-{" + date + "}").orElse("")
                    + "-" + token(header)));
            model.add(subject, values.createIRI(RR + "class"), values.createIRI(RoleOntology.observationClass()));

            reference(model, map, SOSA + "hasSimpleResult", header);
            constant(model, map, SOSA + "observedProperty", header);
            featureOfInterest(model, map, sheet, nameColumn, parent, objectMaps, nameColumns);
        }
    }

    /**
     * Points the observation at its object: a join on the object's name when another sheet describes
     * the objects, the object's own map when the same sheet does.
     */
    private void featureOfInterest(Model model, IRI map, TemplateSheet sheet, String nameColumn,
                                   Optional<Map.Entry<String, Resource>> parent, Map<String, Resource> objectMaps,
                                   Map<String, String> nameColumns) {
        BNode feature = values.createBNode();
        BNode objectMap = values.createBNode();
        model.add(map, values.createIRI(RR + "predicateObjectMap"), feature);
        model.add(feature, values.createIRI(RR + "predicate"), values.createIRI(RoleOntology.featureOfInterest()));
        model.add(feature, values.createIRI(RR + "objectMap"), objectMap);
        Resource own = objectMaps.get(sheet.getName());
        if (own != null) {
            model.add(objectMap, values.createIRI(RR + "parentTriplesMap"), own);
        } else if (parent.isPresent()) {
            BNode join = values.createBNode();
            model.add(objectMap, values.createIRI(RR + "parentTriplesMap"), parent.get().getValue());
            model.add(objectMap, values.createIRI(RR + "joinCondition"), join);
            model.add(join, values.createIRI(RR + "child"), values.createLiteral(nameColumn));
            model.add(join, values.createIRI(RR + "parent"), values.createLiteral(nameColumns.get(parent.get().getKey())));
        }
    }

    private void source(Model model, IRI map, TemplateSheet sheet) {
        BNode source = values.createBNode();
        model.add(map, values.createIRI(RML + "logicalSource"), source);
        model.add(source, values.createIRI(RML + "source"), values.createLiteral(sheet.getName() + ".csv"));
        model.add(source, values.createIRI(RML + "referenceFormulation"), values.createIRI(QL + "CSV"));
    }

    private void reference(Model model, Resource map, String predicate, String header) {
        BNode entry = values.createBNode();
        BNode object = values.createBNode();
        model.add(map, values.createIRI(RR + "predicateObjectMap"), entry);
        model.add(entry, values.createIRI(RR + "predicate"), values.createIRI(predicate));
        model.add(entry, values.createIRI(RR + "objectMap"), object);
        model.add(object, values.createIRI(RML + "reference"), values.createLiteral(header));
    }

    private void constant(Model model, Resource map, String predicate, String text) {
        BNode entry = values.createBNode();
        BNode object = values.createBNode();
        model.add(map, values.createIRI(RR + "predicateObjectMap"), entry);
        model.add(entry, values.createIRI(RR + "predicate"), values.createIRI(predicate));
        model.add(entry, values.createIRI(RR + "objectMap"), object);
        model.add(object, values.createIRI(RR + "constant"), values.createLiteral(text));
    }

    private static boolean describesObjects(TemplateSheet sheet) {
        return sheet.kind().map(SheetKind::describesObjects).orElse(false);
    }

    private static boolean holdsData(TemplateSheet sheet) {
        return sheet.kind().map(SheetKind::holdsData).orElse(false);
    }

    private static Optional<String> column(TemplateSheet sheet, DescribedRole role) {
        return sheet.roles().entrySet().stream().filter(entry -> entry.getValue() == role)
                .map(Map.Entry::getKey).findFirst();
    }

    private static String token(String text) {
        return text.trim().replaceAll("[^\\p{L}\\p{N}_]+", "_");
    }
}
