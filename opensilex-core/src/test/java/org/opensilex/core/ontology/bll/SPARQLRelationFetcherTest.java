package org.opensilex.core.ontology.bll;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.SPARQLRelationFetcher;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.opensilex.sparql.deserializer.SPARQLDeserializers.nodeURI;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class SPARQLRelationFetcherTest extends AbstractMongoIntegrationTest {
    private static SPARQLService sparql;
    private static MongoDBService mongodb;
    private static ScientificObjectDAO dao;

    private static final Path ONTOLOGY_PATH = Paths.get("ontologies", "sparqlRelationFetcherTest.owl");
    private static final URI ONTOLOGY_URI = URI.create("http://example.org/opensilex/test/ontology/sparql-relation-fetcher#");
    private static final URI PROP_1_URI = URI.create(ONTOLOGY_URI + "prop1");
    private static final URI TYPE_1_URI = URI.create(ONTOLOGY_URI + "type1");
    private static final URI TYPE_2_URI = URI.create(ONTOLOGY_URI + "type2");

    @BeforeClass
    public static void beforeTest() throws URISyntaxException, SPARQLException {
        sparql = newSparqlService();
        mongodb = getMongoDBService();
        dao = new ScientificObjectDAO(sparql);
    }

    private static ExperimentModel make(URI experimentUri, String name) throws Exception {
        var experiment = new ExperimentModel();
        experiment.setName(name);
        experiment.setObjective(name + " objective");
        experiment.setUri(experimentUri);
        experiment.setStartDate(LocalDate.parse("2026-09-01"));
        return experiment;
    }

    private static DeviceModel makeDevice(URI deviceURI, String name) {
        var device = new DeviceModel();
        device.setName(name);
        device.setUri(deviceURI);
        device.setType(URI.create(Oeso.Device.getURI()));
        return device;
    }

    private static SPARQLModelRelation makeObjectRelation(URI property, URI object) {
        var relation = new SPARQLModelRelation();
        relation.setProperty(Ontology.property(property));
        relation.setValue(object.toString());
        relation.setType(URI.class);
        return relation;
    }

    private static ScientificObjectModel makeScientificObject(URI uri, String name, URI type, Map<URI, List<URI>> objectRelations) {
        var so = new ScientificObjectModel();
        so.setName(name);
        so.setUri(uri);
        so.setType(type);
        so.setRelations(objectRelations.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> makeObjectRelation(entry.getKey(), value)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        return so;
    }

    private static SelectBuilder makeObjectSelect(URI experiment) {
        var rdfType = makeVar("rdfType");
        var uri = makeVar("uri");
        var name = makeVar("name");
        var experimentNode = nodeURI(experiment);

        return new SelectBuilder()
                .addVar(rdfType)
                .addVar(uri)
                .addVar(name)
                .addWhere(rdfType, Ontology.subClassAny, Oeso.ScientificObject)
                .addGraph(experimentNode, new WhereBuilder()
                        .addWhere(uri, RDFS.label, name)
                        .addWhere(uri, RDF.type, rdfType));
    }

    /**
     * The SPARQLRelationFetcher should not throw when fetching mixed mono- and multivalued properties. This test
     * follows this setup:
     *
     * <ul>
     *     <li>Create an object property on Scientific Objects (prop1)</li>
     *     <li>Create two Scientific Object types (type1 and type2)</li>
     *     <li>Add a <strong>multivalued</strong> restriction in type1 on prop1</li>
     *     <li>Add a <strong>monovalued</strong> restriction in type2 on prop1</li>
     *     <li>Create an experiment</li>
     *     <li>Create two Scientific Objects of types type1 and type2 in the experiment</li>
     *     <li>Set values for the relation prop1 in both Scientific Objects</li>
     * </ul>
     *
     * This specific combination previously failed with a NullPointerException when trying to populate the models with
     * the fetched relations. That was because the type <code>type2</code> is not associated with any multivalued
     * restriction, but the code to associate the multivalued relations is still executed because <code>type1</code> has
     * a multivalued restriction on <code>prop1</code>.
     */
    @Test
    public void testMixedMultiMonoValuedProperty() throws Exception {
        sparql.loadOntology(ONTOLOGY_URI, OpenSilex.getResourceAsStream(ONTOLOGY_PATH.toString()), Lang.RDFXML);

        var experimentUri = URI.create("http://example.org/opensilex/test/experiment/1");
        var experimentNode = nodeURI(experimentUri);
        var device1Uri = URI.create("http://example.org/opensilex/test/device/1");
        var device2Uri = URI.create("http://example.org/opensilex/test/device/2");
        var object1Uri = URI.create("http://example.org/opensilex/test/object/1");
        var object2Uri = URI.create("http://example.org/opensilex/test/object/2");
        sparql.create(make(experimentUri, "experiment"));
        sparql.create(makeDevice(device1Uri, "device1"));
        sparql.create(makeDevice(device2Uri, "device2"));
        sparql.create(experimentNode, makeScientificObject(object1Uri, "object1", TYPE_1_URI, Map.of(PROP_1_URI, List.of(device1Uri, device2Uri))));
        sparql.create(experimentNode, makeScientificObject(object2Uri, "object2", TYPE_2_URI, Map.of(PROP_1_URI, List.of(device1Uri))));

        var initialModels = List.of(
                makeScientificObject(object1Uri, "object1", TYPE_1_URI, Map.of()),
                makeScientificObject(object2Uri, "object2", TYPE_2_URI, Map.of())
        );
        var select = makeObjectSelect(experimentUri);
        var relationFetcher = new SPARQLRelationFetcher<>(
                sparql,
                ScientificObjectModel.class,
                experimentNode,
                select,
                initialModels);
        relationFetcher.updateModels();
    }
}
