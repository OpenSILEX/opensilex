/*
 *  *************************************************************************************
 *  ScientificObjectNameIntegerConvertMigrationTest.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.migration;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.update.OpenSilexModuleUpdate;


import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rcolin
 */
public class ScientificObjectNameIntegerConvertMigrationTest extends AbstractMongoIntegrationTest {

    private static SPARQLService sparql;
    private static Node experimentGraph;
    private static Node globalObjectGraph;
    private static ExperimentModel experiment;

    private static final int NB_GOOD_OS = 10;
    private static final int TOTAL_OS_NB = 20;

    @BeforeClass
    public static void setup() throws Exception {

        // init sparql and experiment
        sparql = newSparqlService();

        experiment = new ExperimentModel();
        experiment.setName("ScientificObjectNameIntegerConvertMigrationTest");
        experiment.setObjective(experiment.getName());
        experiment.setStartDate(LocalDate.now());
        sparql.create(experiment);
        experimentGraph = NodeFactory.createURI(experiment.getUri().toString());

        ScientificObjectDAO dao = new ScientificObjectDAO(sparql,getMongoDBService());
        URI osType = URI.create(Oeso.ScientificObject.getURI());

        // create object which should not be affected by the bug
        // the test must ensure that any data unaffected by the bug must stay unchanged
        List<ScientificObjectModel> models = new ArrayList<>(NB_GOOD_OS);
        for (int i = 0; i < NB_GOOD_OS; i++) {
            ScientificObjectModel model = new ScientificObjectModel();
            model.setName("name_"+i);
            model.setType(osType);
            models.add(model);
        }
        dao.create(models,experiment.getUri());
        dao.copyIntoGlobalGraph(models); // good copy method

        // create Object affected by the bug
        models.clear();
        for (int i = 0; i < NB_GOOD_OS; i++) {
            ScientificObjectModel model = new ScientificObjectModel();
            model.setName(""+i); // use a name with only digit
            model.setType(osType);
            models.add(model);
        }
        globalObjectGraph = sparql.getDefaultGraph(ScientificObjectModel.class);
        dao.create(models,experiment.getUri());

        Node typeNode = SPARQLDeserializers.nodeURI(osType.toString());

        // performs a manual copy with thue buggy method
        // here label is directly send to the update
        UpdateBuilder copyUpdate = new UpdateBuilder();
        for(ScientificObjectModel model :models){
            Node soNode = SPARQLDeserializers.nodeURI(model.getUri());
            copyUpdate.addInsert(globalObjectGraph, soNode, RDF.type, typeNode);
            copyUpdate.addInsert(globalObjectGraph, soNode, RDFS.label, model.getName());
        }
        sparql.executeUpdateQuery(copyUpdate);

        // make sure count and primitive count are OK
        Assert.assertEquals(TOTAL_OS_NB, sparql.count(experimentGraph, ScientificObjectModel.class));
        Assert.assertEquals(TOTAL_OS_NB, sparql.count(globalObjectGraph, ScientificObjectModel.class));

        Assert.assertEquals(TOTAL_OS_NB, sparql.searchPrimitives(experimentGraph, null, RDFS.label, String.class).size());

        // only object copied via copyIntoGlobalGraph() method should have the good type
        Assert.assertEquals(NB_GOOD_OS, sparql.searchPrimitives(globalObjectGraph, null, RDFS.label, String.class).size());
        Assert.assertEquals(NB_GOOD_OS, sparql.searchPrimitives(globalObjectGraph, null, RDFS.label, Integer.class).size());

        // build migration and run it
        OpenSilexModuleUpdate update = new ScientificObjectNameIntegerConvertMigration();
        update.setOpensilex(getOpensilex());
        update.execute();
    }

    @Test
    public void noUpdateInExperimentGraph() throws Exception {

        Assert.assertEquals(TOTAL_OS_NB, sparql.count(experimentGraph, ScientificObjectModel.class));

        // ensures that all labels are still of type String
        Assert.assertEquals(TOTAL_OS_NB, sparql.searchPrimitives(experimentGraph, null, RDFS.label, String.class).size());
    }

    @Test
    public void updateInGlobalGraph() throws Exception {

        Assert.assertEquals(20, sparql.count(globalObjectGraph, ScientificObjectModel.class));

        // ensures that all labels are now of type String
        Assert.assertEquals(20, sparql.searchPrimitives(globalObjectGraph, null, RDFS.label, String.class).size());
        Assert.assertTrue(sparql.searchPrimitives(globalObjectGraph, null, RDFS.label, Integer.class).isEmpty());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        sparql.clearGraph(ScientificObjectModel.class);
        sparql.clearGraph(ExperimentModel.class);
        sparql.clearGraph(experiment.getUri());
    }
}