package org.opensilex.migration;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.OpenSilex;
import org.opensilex.security.account.api.AccountAPI;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.api.UserAPI;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * This migration has for goal to add the Account credential to all users that had the User credentials.
 * User Credentials still exists, but are not used in the interface, it only grants access to somme API services
 * <p>
 * The request to do that is :
 * <pre>
 *     PREFIX os-sec: <http://www.opensilex.org/security#>
 * INSERT
 *        {
 * 		GRAPH <http://opensilex.test/set/project> {
 *     		?profilUri os-sec:hasCredential "account-access";
 *        }
 *    }
 * 	WHERE
 *        {
 * 		GRAPH <http://opensilex.test/set/project> {
 *     		?profilUri a os-sec:Profile ;
 *     		os-sec:hasCredential "user-access";
 *     	    NOT EXISTS {
 *              ?profilUri os-sec:hasCredential "account-modification";
 *          }
 *        }
 *      };
 * INSERT
 *    {
 * 		GRAPH <http://opensilex.test/set/project> {
 *     		?profilUri os-sec:hasCredential "account-modification";
 *        }
 *    }
 * 	WHERE
 *        {
 * 		GRAPH <http://opensilex.test/set/project> {
 *     		?profilUri a os-sec:Profile ;
 *     		os-sec:hasCredential "user-modification";
 *     	    NOT EXISTS {
 *              ?profilUri os-sec:hasCredential "account-modification";
 *          }
 *        }
 *      }
 * </pre>
 */
public class AddAccountCredentialsToProfilWithUserCredential implements OpenSilexModuleUpdate {
    public static final String ACCOUNT_ACCESS_CREDENTIAL = "account-access";
    public static final String USER_ACCESS_CREDENTIAL = "user-access";

    private OpenSilex opensilex;
    private SPARQLService sparql;
    private final Logger logger = LoggerFactory.getLogger(AddAccountCredentialsToProfilWithUserCredential.class);

    private Node graph;
    private Node credentialPredicate;
    private Node profilType;
    private Var profilVar;

    private void migrate() throws OpensilexModuleUpdateException {
        try {

            graph = sparql.getDefaultGraph(ProfileModel.class);
            credentialPredicate = SecurityOntology.hasCredential.asNode();
            profilType = sparql.getRDFType(ProfileModel.class).asNode();
            profilVar = makeVar("profilURI");

            UpdateBuilder accessQuery = getUpdateQuery(ACCOUNT_ACCESS_CREDENTIAL, USER_ACCESS_CREDENTIAL);
            UpdateBuilder modificationQuery = getUpdateQuery(AccountAPI.CREDENTIAL_ACCOUNT_MODIFICATION_ID, UserAPI.CREDENTIAL_USER_MODIFICATION_ID);

            sparql.startTransaction();
            try {
                sparql.executeUpdateQuery(accessQuery);
                System.out.println(accessQuery.buildRequest());
                sparql.executeUpdateQuery(modificationQuery);
                sparql.commitTransaction();
            } catch (Exception e) {
                rollback(e);
            }
        } catch (Exception e) {
            throw new OpensilexModuleUpdateException("error while adding new credentials. No changes was saved on the rdf database", e);
        }

        logger.info("Account credentials were added and saved in the rdf database");
    }

    private UpdateBuilder getUpdateQuery(String accountCredential, String userCredential) {
        Element hasNotAccountCredentialYet = new WhereBuilder()
                .addWhere(profilVar, credentialPredicate, accountCredential)
                .getWhereHandler()
                .getElement();

        return new UpdateBuilder()
                .addInsert(graph, profilVar, credentialPredicate, accountCredential)
                .addWhere(profilVar, RDF.type.asNode(), profilType)
                .addWhere(profilVar, credentialPredicate, userCredential)
                .addFilter( new E_NotExists(hasNotAccountCredentialYet) );
    }

    private void rollback(Exception e) throws OpensilexModuleUpdateException {
        try {
            sparql.rollbackTransaction();
            throw new OpensilexModuleUpdateException("error while adding new credentials. No changes was saved on the rdf database", e);
        } catch (OpensilexModuleUpdateException osEx) {
            throw osEx;
        } catch (Exception ex) {
            throw new OpensilexModuleUpdateException("critical error while adding new credentials and trying to roll-back. Partial changes may be saved on the rdf database", e);
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
        migrate();
    }

}
