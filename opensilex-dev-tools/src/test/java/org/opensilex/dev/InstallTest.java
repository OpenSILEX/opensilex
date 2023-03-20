package org.opensilex.dev;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.OA;
import org.bson.Document;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.phis.ontology.OesoExt;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.ontology.OesoSecurity;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Test that OpenSILEX installation works.
 * The following configuration are tested :
 * <ul>
 *     <li>RDF4J repository</li>
 *     <li>RDF4J in memory repository</li>
 *     <li>Graph repository</li>
 * </ul>
 *
 * All test need a running MongoDB server running. See config used
 * @author rcolin
 */
@Ignore
public class InstallTest {

    @Test
    public void installWithInMemoryRepository() throws Exception {
        testInstall("opensilex_rdf4j_in_memory_install_test.yml",false);
    }

    /**
     * This test need a running RDF4J server instance in order to success. See the configuration used
     */
    @Test
    public void installWithRdf4jRepository() throws Exception {
        testInstall("opensilex_rdf4j_install_test.yml",false);
    }

    /**
     * This test need a running graphdb server instance in order to success. See the configuration used
     */
    @Test
    public void installWithGraphDbRepository() throws Exception {
        testInstall("opensilex_graphdb_install_test.yml",false);
    }


    private void testInstall(String configFile, boolean reset) throws Exception {

        Path configPath = Paths.get("src","test","resources","configs",configFile);

        // use custom config
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);
        customArgs.put(OpenSilex.CONFIG_FILE_ARG_KEY,configPath.toAbsolutePath().toString());

        OpenSilex openSilex = OpenSilex.createInstance(customArgs);

        // load RDF4JServiceFactory in order to manipulate Repository (for existence checking and deletion)
        SPARQLServiceFactory sparqlServiceFactory = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = sparqlServiceFactory.provide();
        RDF4JServiceFactory rdf4JServiceFactory = (RDF4JServiceFactory) sparqlServiceFactory;

        MongoDBService mongoDBService = openSilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE,MongoDBService.class);
        try{
            openSilex.install(reset);
            openSilex.getModuleByClass(SecurityModule.class).createDefaultSuperAdmin();

            // ensure that initial objects and ontologies are created
            assertDefaultOntologiesExists(sparql);
            assertDefaultUserExists(sparql);
            assertDefaultProfileExists(sparql);
            assertDefaultGermplasmExists(sparql);
            assertDefaultMethodExists(sparql);
            assertDefaultVariableGroupExists(sparql);
            assertDefaultProvenanceExists(mongoDBService);
            assertRepositoryExists(rdf4JServiceFactory);

        }finally {
            rdf4JServiceFactory.deleteRepository();
        }

    }

    private void assertRepositoryExists(RDF4JServiceFactory rdf4JServiceFactory){
        Repository repository = rdf4JServiceFactory.getRepository();
        Assert.assertTrue(repository.isInitialized());
        repository.shutDown();
    }

    private void assertDefaultMethodExists(SPARQLService sparql) throws Exception {
        List<MethodModel> models = sparql.search(MethodModel.class,null);
        Assert.assertFalse(CollectionUtils.isEmpty(models));
    }

    private void assertDefaultVariableGroupExists(SPARQLService sparql) throws Exception {
        List<VariablesGroupModel> models = sparql.search(VariablesGroupModel.class,null);
        Assert.assertFalse(CollectionUtils.isEmpty(models));
    }

    private void assertDefaultUserExists(SPARQLService sparql) throws Exception {
        List<AccountModel> models = sparql.search(AccountModel.class,null);
        Assert.assertFalse(CollectionUtils.isEmpty(models));
    }

    private void assertDefaultProfileExists(SPARQLService sparql) throws Exception {
        List<ProfileModel> models = sparql.search(ProfileModel.class,null);
        Assert.assertFalse(CollectionUtils.isEmpty(models));
    }

    private void assertDefaultGermplasmExists(SPARQLService sparql) throws Exception {
        List<SpeciesModel> models = sparql.search(SpeciesModel.class,null);
        Assert.assertFalse(CollectionUtils.isEmpty(models));
    }

    private void assertDefaultProvenanceExists(MongoDBService mongodb){
        List<ProvenanceModel> models = mongodb.search(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new Document(), Collections.emptyList());
        Assert.assertFalse(CollectionUtils.isEmpty(models));
    }

    private void assertDefaultOntologiesExists(SPARQLService sparql){

        List<String> ontologyGraphs = Arrays.asList(
                Oeso.NS,
                Oeev.NS,
                OesoSecurity.NS,
                OesoExt.NS,
                OWL.NAMESPACE,
                Time.NS,
                DCTERMS.NAMESPACE,
                OA.NS
        );

        RepositoryConnection connection = sparql.getRepositoryConnection();
        for(String graph : ontologyGraphs){

            // remove #  from end of graph if found
            String realGraph = graph.charAt(graph.length()-1) == '#' ?
                    StringUtils.chop(graph) : graph;

            IRI graphIRI = connection.getValueFactory().createIRI(realGraph);
            Assert.assertTrue(
                    "No RDF triple found from ontology graph "+realGraph,
                    connection.hasStatement(null, null, null, false, graphIRI)
            );
        }
    }

}