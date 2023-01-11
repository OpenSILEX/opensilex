package org.opensilex.migration;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;


import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class AgentsMigrateToAccountAndPersons implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;

    /**
     * <pre>
     * modify every users to correspond to the new model.
     * Before : A user is a foaf:Agent with : mail (id), hashed passWord, admin info, language info, firstName, lastName
     * After : A user is :
     *          A foaf:OnlineAccount with an accountName (id), hashedpassword, admin info and language info
     *          And a foaf:Person with fistName, lastName and mail.
     *          They are both linked with foaf:account
     *
     * Querry to execute:
     *
     * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
     * PREFIX os-sec:<http://www.opensilex.org/security#>
     *
     * 	DELETE { GRAPH <http://opensilex.dev/set/user>
     *        {
     * 		?userUri 	rdf:type 				foaf:Agent;
     * 			 		foaf:firstName			?prenom;
     * 			 		foaf:lastName 			?nom;
     * 			 		foaf:mbox				?identifiantMail
     *    } }
     * 	INSERT { GRAPH <http://opensilex.dev/set/user>
     *    {
     * 		?userUri 		rdf:type 			foaf:OnlineAccount;
     * 			 			foaf:accountName	?identidiantMail.
     *
     *
     * 		?personURI 	rdf:type				foaf:Person;
     * 					foaf:account 			?userUri;
     * 				 	foaf:firstName 			?prenom;
     * 				 	foaf:lastName 			?nom;
     * 				 	foaf:mbox 				?identidiantMail.
     *    } }
     * 	WHERE { GRAPH <http://opensilex.dev/set/user> {
     * 		?userUri 	rdf:type				foaf:Agent;
     * 					foaf:mbox				?identidiantMail;
     * 					foaf:firstName 			?prenom;
     * 					foaf:lastName 			?nom.
     * 		OPTIONAL{?userUri	os-sec:hasPasswordHash			?pwdH}
     * 		OPTIONAL{?userUri	os-sec:isAdmin			?admin}
     * 		OPTIONAL{?userUri	os-sec:hasLanguage 		?language;}
     * 		BIND(URI(CONCAT(STR(?userUri), "/Person")) as ?personURI)
     *        } };
     *
     * </pre>
     */
    public void modifyingAllUsersWithNewModel(SPARQLService sparql) throws OpensilexModuleUpdateException {

        try {
            //user Graphe node
            Node usersGraphNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(AccountModel.class));

            // Variables
            Var userUriVar = makeVar("userUri");
            Var personUriVar = makeVar("personUri");

            Var identifiantMailVar = makeVar("identifiantMail");
            Var passwordHashVar = makeVar("passwordHash");
            Var adminVar = makeVar("admin");
            Var languageVar = makeVar("language");

            Var prenomVar = makeVar("prenom");
            Var nomVar = makeVar("nom");

            //delete the old association with Agent and first/lastName
            UpdateBuilder query = new UpdateBuilder()
                    .addDelete(usersGraphNode, userUriVar, RDF.type.asNode(), FOAF.Agent.asNode())
                    .addDelete(usersGraphNode, userUriVar, FOAF.firstName.asNode(), prenomVar)
                    .addDelete(usersGraphNode, userUriVar, FOAF.lastName.asNode(), nomVar)
                    .addDelete(usersGraphNode, userUriVar, FOAF.mbox.asNode(), identifiantMailVar);

            //insert Online account
            query.addInsert(usersGraphNode, userUriVar, RDF.type.asNode(), FOAF.OnlineAccount.asNode())
                    .addInsert(usersGraphNode, userUriVar, FOAF.accountName.asNode(), identifiantMailVar);

            //insert Person
            query.addInsert(usersGraphNode, personUriVar, RDF.type.asNode(), FOAF.Person.asNode())
                    .addInsert(usersGraphNode, personUriVar, FOAF.account.asNode(), userUriVar)
                    .addInsert(usersGraphNode, personUriVar, FOAF.firstName.asNode(), prenomVar)
                    .addInsert(usersGraphNode, personUriVar, FOAF.lastName.asNode(), nomVar)
                    .addInsert(usersGraphNode, personUriVar, FOAF.mbox.asNode(), identifiantMailVar);

            //where clause of the update Query
            WhereBuilder where = new WhereBuilder()
                    .addWhere(userUriVar, RDF.type.asNode(), FOAF.Agent.asNode())
                    .addWhere(userUriVar, FOAF.mbox.asNode(), identifiantMailVar)
                    .addWhere(userUriVar, FOAF.firstName.asNode(), prenomVar)
                    .addWhere(userUriVar, FOAF.lastName.asNode(), nomVar)
                    .addOptional(userUriVar, SecurityOntology.hasPasswordHash.asNode(), passwordHashVar)
                    .addOptional(userUriVar, SecurityOntology.isAdmin.asNode(), adminVar)
                    .addOptional(userUriVar, SecurityOntology.hasLanguage.asNode(), languageVar);
            //Bind function of the where clause
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
            Expr expr = exprFactory.iri(new ExprFactory().concat(new ExprFactory().str(userUriVar.asNode()), "/Person"));
            where.addBind(expr, personUriVar);
            //Add the where clause with the graph
            query.addGraph(usersGraphNode, where);


            try {
                sparql.executeUpdateQuery(query);
            } catch (SPARQLException e) {
                LoggerFactory.getLogger(AccountDAO.class).error("Error while migrating users ", e);
            }

        } catch (Exception e) {
            throw new OpensilexModuleUpdateException(e.getMessage(), e);
        }
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "Updates the users to correspond to the new model, with account and persons";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        modifyingAllUsersWithNewModel(sparql);
    }
}
