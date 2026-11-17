//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.apache.jena.vocabulary.RDF;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectAPI;
import org.opensilex.core.scientificObject.bll.ScientificObjectLogic;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates scientific objects in the experiment of the import, the way the platform's scientific object
 * API does: the name and type on the model, every other value as a relation the platform checks against
 * the ontology, then the scientific object logic, which checks the experiment, the factor levels and the
 * names. A parent of the same step is created before its children. No geometry and no move are written.
 *
 * @author Arnaud Charleroy
 */
public class ScientificObjectWriter implements ResourceWriter {

    public static final String STEP = "scientific-objects";

    /**
     * The most objects one step creates: the platform creates them one by one, each with its checks, about
     * 170 ms each on an embedded triplestore (500 objects took 84 s in one request), and a longer step would
     * hold the request too long.
     */
    public static final int MAX_OBJECTS = 500;

    static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());
    private static final URI TYPE = URI.create(RDF.type.getURI());

    @Override
    public String step() {
        return STEP;
    }

    @Override
    public Set<URI> rootClasses() {
        return Set.of(SCIENTIFIC_OBJECT);
    }

    @Override
    public String credentialId() {
        return ScientificObjectAPI.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID;
    }

    @Override
    public List<String> after() {
        return List.of(ExperimentWriter.STEP, FactorWriter.STEP);
    }

    @Override
    public Optional<ReportMessage> whyNot(NodeComparison node, WriteContext context) {
        return context.experiment() == null ? Optional.of(CreationMessages.noExperimentForObjects()) : Optional.empty();
    }

    @Override
    public List<WriteProblem> check(List<NodeComparison> nodes, WriteContext context) throws Exception {
        List<WriteProblem> problems = new ArrayList<>();
        if (nodes.size() > MAX_OBJECTS) {
            problems.add(WriteProblem.ofStep(CreationMessages.tooMany(nodes.size(), MAX_OBJECTS)));
            return problems;
        }
        OntologyDAO ontology = new OntologyDAO(context.sparql());
        Map<URI, ClassModel> classes = new HashMap<>();
        Set<String> names = new HashSet<>();
        for (NodeComparison node : nodes) {
            Optional<ReportMessage> reason = whyNot(node, context);
            if (reason.isPresent()) {
                problems.add(WriteProblem.of(node, reason.get()));
                continue;
            }
            ScientificObjectModel model = modelOf(node);
            if (!names.add(model.getName())) {
                problems.add(WriteProblem.of(node, CreationMessages.sameName(model.getName())));
                continue;
            }
            // The relations to objects of this step cannot be checked before those exist: the platform
            // checks them when it writes.
            List<RDFObjectRelationDTO> relations = relationsOf(node, Map.of(), false);
            try {
                ClassModel classModel = classes.computeIfAbsent(model.getType(), type -> classModel(ontology, type,
                        context));
                RDFObjectDTO.validatePropertiesAndAddToObject(context.experiment(), classModel, model, relations,
                        ontology);
            } catch (Exception e) {
                problems.add(WriteProblem.of(node, CreationMessages.refused(String.valueOf(e.getMessage()))));
            }
        }
        return problems;
    }

    @Override
    public List<URI> write(List<NodeComparison> nodes, WriteContext context) throws Exception {
        if (nodes.size() > MAX_OBJECTS) {
            throw new IllegalArgumentException(CreationMessages.tooMany(nodes.size(), MAX_OBJECTS).getEnglish());
        }
        ScientificObjectLogic logic = new ScientificObjectLogic(context.sparql(), context.nosql(), context.fs());
        Map<String, URI> created = new LinkedHashMap<>();
        // Each object is written in its own transaction by the platform; this one holds them all, so that a
        // refusal leaves none written.
        context.sparql().startTransaction();
        try {
            for (NodeComparison node : parentsFirst(nodes)) {
                URI uri = logic.createScientificObject(modelOf(node), context.experiment(),
                        relationsOf(node, created, true), null, context.user());
                created.put(node.node().getKey(), uri);
            }
            context.sparql().commitTransaction();
        } catch (Exception e) {
            context.sparql().rollbackTransaction(e);
            throw e;
        }
        return List.copyOf(created.values());
    }

    private static ScientificObjectModel modelOf(NodeComparison node) {
        ScientificObjectModel model = new ScientificObjectModel();
        model.setName(NodeValues.of(node).literal(ExperimentWriter.NAME).orElse(node.node().getName()));
        model.setType(node.classUri());
        return model;
    }

    /**
     * Every value but the name and type, as the platform's relations: a literal as the file gives it, read
     * for its datatype, a link as the URI of what it points to.
     *
     * @param created  the objects of this step already written, by node key
     * @param withRows whether the links to rows of this step are given; they are left out of a check
     */
    private static List<RDFObjectRelationDTO> relationsOf(NodeComparison node, Map<String, URI> created,
                                                          boolean withRows) {
        NodeValues values = NodeValues.of(node);
        List<RDFObjectRelationDTO> relations = new ArrayList<>();
        for (URI property : values.properties()) {
            if (property.equals(ExperimentWriter.NAME) || property.equals(TYPE)) {
                continue;
            }
            values.literals(property).forEach(value -> relations.add(new RDFObjectRelationDTO(property, value, false)));
            values.targets(property).forEach(target -> relations.add(new RDFObjectRelationDTO(property,
                    platformForm(target), false)));
            if (withRows) {
                values.rows(property).stream().map(created::get).filter(uri -> uri != null)
                        .forEach(uri -> relations.add(new RDFObjectRelationDTO(property, platformForm(uri), false)));
            }
        }
        return relations;
    }

    /**
     * A URI as the platform writes it in its models, with its prefix: some of its checks compare URIs as
     * they are, and a full URI never equals a prefixed one.
     */
    private static String platformForm(URI uri) {
        return SPARQLDeserializers.getShortURI(uri.toString());
    }

    /** The objects in an order where every parent of this step comes before the objects pointing to it. */
    private static List<NodeComparison> parentsFirst(List<NodeComparison> nodes) {
        Map<String, NodeComparison> byKey = new LinkedHashMap<>();
        nodes.forEach(node -> byKey.put(node.node().getKey(), node));
        List<NodeComparison> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String key : byKey.keySet()) {
            place(key, byKey, ordered, placed, visiting);
        }
        return ordered;
    }

    private static void place(String key, Map<String, NodeComparison> byKey, List<NodeComparison> ordered,
                              Set<String> placed, Set<String> visiting) {
        if (placed.contains(key) || !visiting.add(key)) {
            return;
        }
        for (String parent : NodeValues.of(byKey.get(key)).rowsPointedTo()) {
            if (byKey.containsKey(parent)) {
                place(parent, byKey, ordered, placed, visiting);
            }
        }
        placed.add(key);
        ordered.add(byKey.get(key));
    }

    private static ClassModel classModel(OntologyDAO ontology, URI type, WriteContext context) {
        try {
            return ontology.getClassModel(type, SCIENTIFIC_OBJECT, context.user().getLanguage());
        } catch (Exception e) {
            throw new IllegalStateException("The class " + type + " is not known to the platform", e);
        }
    }
}
