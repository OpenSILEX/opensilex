package org.opensilex.migration;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.bson.Document;
import org.opensilex.OpenSilex;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * this migration concern the following predicates :
 * Project :
 *      - vocabulary:hasScientificContact
 *      - vocabulary:hasAdministrativeContact
 *      - vocabulary:hasCoordinator
 * Experiment :
 *      - vocabulary:hasScientificSupervisor
 *      - vocabulary:hasTechnicalSupervisor
 * Device :
 *      - vocabulary:personInCharge
 * Provenance :
 *        {
 *            Agents : [ {
 *                        rdfType: "http://www.opensilex.org/vocabulary/oeso#Operator"
 *                        uri: Account -> Person
 *                        }, ... ]
 *        }
 */
public class ObjectMigrationFromAccountToPerson implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;

    private SPARQLService sparql;
    private MongoDBService mongodb;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * looks for change the Objects of predicates from an Account to the person who's associated with.
     * <pre>
     * exemple of request for changing Object of the vocabulary:hasScientificContact predicate :
     *
     * PREFIX vocabulary: <http://www.opensilex.org/vocabulary/oeso#>
     * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
     *
     *DELETE {
     *GRAPH <http://opensilex.test/set/project> {
     *?subjectUri vocabulary:hasScientificContact ?accountUri .
     *}
     *}
     *INSERT {
     *GRAPH <http://opensilex.test/set/project> {
     *?subjectUri vocabulary:hasScientificContact ?personUri .
     *}
     *}
     *WHERE
     *{ ?subType (rdfs:subClassOf)* vocabulary:Project .
     *?subjectUri  a                  ?subType ;
     *vocabulary:hasScientificContact  ?accountUri .
     *?accountUri  a                  foaf:OnlineAccount .
     *?personUri  foaf:account        ?accountUri
     *}
     *   </pre>
     */
    private void modifyingExpectedObjectsFromAccountTooPerson() {
        try {
            sparql.startTransaction();
            mongodb.startTransaction();

            // Project
            Node projectGraph = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ProjectModel.class));
            Node rdfTypeProject = sparql.getRDFType(ProjectModel.class).asNode();
            modifyObjectFromAccountToPerson(rdfTypeProject, Oeso.hasScientificContact.asNode(), projectGraph);
            modifyObjectFromAccountToPerson(rdfTypeProject, Oeso.hasAdministrativeContact.asNode(), projectGraph);
            modifyObjectFromAccountToPerson(rdfTypeProject, Oeso.hasCoordinator.asNode(), projectGraph);

            // Experiment
            Node experimentGraph = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ExperimentModel.class));
            Node rdfTypeExperiment = sparql.getRDFType(ExperimentModel.class).asNode();
            modifyObjectFromAccountToPerson(rdfTypeExperiment, Oeso.hasScientificSupervisor.asNode(), experimentGraph);
            modifyObjectFromAccountToPerson(rdfTypeExperiment, Oeso.hasTechnicalSupervisor.asNode(), experimentGraph);

            // Device
            Node deviceGraph = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(DeviceModel.class));
            modifyObjectFromAccountToPerson(Oeso.Device.asNode(), Oeso.personInCharge.asNode(), deviceGraph);

            //provenance
            ProvenanceDAO provenanceDAO = new ProvenanceDAO(mongodb, sparql);
            List<ProvenanceModel> provenances = mongodb.search(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new Document(), null);
            List<AccountModel> accountList = new AccountDAO(sparql).search(".*", null, null, null).getList();
            Map<URI, AccountModel> accounts = accountList.stream().collect(Collectors.toMap(AccountModel::getUri, Function.identity()));
            String operatorURI = URIDeserializer.getExpandedURI(Oeso.Operator.getURI());
            for (ProvenanceModel provenance : provenances) {
                migrateProvenanceOperators(provenance, accounts, operatorURI, provenanceDAO);
            }

            sparql.commitTransaction();
            mongodb.commitTransaction();
            logger.info("Migration effectuée avec succès");
        } catch (Exception e){
            try {
                sparql.rollbackTransaction();
                mongodb.rollbackTransaction();
                logger.error("Erreur lors de la migration des données, aucun changement n'as été fait sur la base de donnée", e);
            } catch(Exception exception){
                logger.error("Erreur critique lors de la migration des données, la base de donnée peut ne plus être dans un état stable, des données ont pu être perdues", exception);
            }
        }
    }

    private void modifyObjectFromAccountToPerson(Node subjectRdfType, Node predicate, Node graph) throws SPARQLException {
        Var subjectVar = makeVar("subjectUri");
        Var accountVar = makeVar("accountUri");
        Var personVar = makeVar("personUri");
        Var typeVar = makeVar("subType");

        //delete the old association with Agent and first/lastName
        UpdateBuilder query = new UpdateBuilder()
                .addDelete(graph, subjectVar, predicate, accountVar);

        //insert Online account
        query.addInsert(graph, subjectVar, predicate, personVar);

        //where clauses of the update Query
        query   .addWhere(typeVar, Ontology.subClassAny, subjectRdfType)
                .addWhere(subjectVar, RDF.type.asNode(), typeVar)
                .addWhere(subjectVar, predicate, accountVar)
                .addWhere(accountVar, RDF.type.asNode(), FOAF.OnlineAccount.asNode())
                .addWhere(personVar, FOAF.account.asNode(), accountVar);

        sparql.executeUpdateQuery(query);
    }

    private void migrateProvenanceOperators(ProvenanceModel provenance, Map<URI, AccountModel> accounts, String operatorPredicateURI, ProvenanceDAO provenanceDAO) throws NoSQLInvalidURIException {
        Collection<AgentModel> operators = new ArrayList<>();
        if ( Objects.isNull(provenance.getAgents()) ) {
            return;
        }

        provenance.getAgents().forEach( agentModel -> {
            String agentType = URIDeserializer.getExpandedURI(agentModel.getRdfType());
            if (agentType.equals(operatorPredicateURI)){
                operators.add(agentModel);
            }
        });

        for (AgentModel operator : operators) {


            AccountModel operatorAccount = accounts.get(operator.getUri());
            if (Objects.nonNull(operatorAccount)) {
                PersonModel holderOfTheOperatorAccount = operatorAccount.getHolderOfTheAccount();
                if (Objects.nonNull(holderOfTheOperatorAccount)) {
                    operator.setUri(holderOfTheOperatorAccount.getUri());
                    provenanceDAO.update(provenance);
                } else {
                    logger.error("le compte " + operatorAccount.getUri() + " n'as pas de personne associée, migration impossible");
                }
            } else {
                logger.error(" migration impossible, aucun compte trouvé pour l'uri : " + operator.getUri());
            }

        }
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "change the type of Objects for some predicates from a foaf:OnlineAccount to the foaf:Person who's associated with by the foaf:account predicate ";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();
        mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        modifyingExpectedObjectsFromAccountTooPerson();
    }


}
