//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.List;

/**
 * PersonDAO is used to manipulate PersonModel and CRUD persons data in the rdf database.
 * @author Yvan Roux
 */
public class PersonDAO {

    private final SPARQLService sparql;

    public PersonDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     *  create a Person in dataBase with the given uri, firstname, lastname and email, only email could be NULL.
     * @param email : optional information only, not used for any user or account authentication
     * @return the personModel of the created Person.
     */
    public PersonModel create(
            URI uri,
            String firstName,
            String lastName,
            String email
    ) throws Exception {
        PersonModel person = new PersonModel();
        person.setUri(uri);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        if (email != null) {
            person.setEmail(new InternetAddress(email));
        }
        sparql.create(person);

        return person;
    }

    /** @return  the Person linked to an account if this person exist, null otherwise. */
    public PersonModel getPersonFromAccount(URI accountURI) throws Exception {
        SPARQLClassObjectMapper<PersonModel> mapper = sparql.getForClass(PersonModel.class);
        List<PersonModel> results =  sparql.search(
                PersonModel.class,
                null,
                (SelectBuilder select ) -> select.addWhere(mapper.getURIFieldVar(), FOAF.account, SPARQLDeserializers.nodeURI(accountURI))
        );
        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    /** @return  the account linked to a Person if this account exist, null otherwise. */
    public AccountModel getAccountFromPerson(URI personURI) throws Exception {
        SPARQLClassObjectMapper<AccountModel> mapper = sparql.getForClass(AccountModel.class);
        List<AccountModel> results =  sparql.search(
                AccountModel.class,
                null,
                (SelectBuilder select ) -> select.addWhere(SPARQLDeserializers.nodeURI(personURI), FOAF.account, mapper.getURIFieldVar())
        );
        if (results.isEmpty())
            return null;

        return results.get(0);
    }

    /** link a Person and its account with the foaf:account predicate in the rdf database*/
    public void setAccount(URI personURI, URI accountURI) throws Exception {
        UpdateBuilder update = new UpdateBuilder();
        update.addInsert(sparql.getDefaultGraph(PersonModel.class), SPARQLDeserializers.nodeURI(personURI), FOAF.account, SPARQLDeserializers.nodeURI(accountURI));
        sparql.executeUpdateQuery(update);
    }

    public boolean hasAnAccount(URI personURI) throws Exception {
        return getAccountFromPerson(personURI) != null;
    }

    /** Delete the following triplet -> personUri, foaf:account, accountUri */
    private void deleteAccountLink(URI personUri, URI accountUri) throws SPARQLException {
        Node usersGraphNode = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(PersonModel.class));
        Node personUriNode = SPARQLDeserializers.nodeURI(personUri);
        Node accountUriNode = SPARQLDeserializers.nodeURI(accountUri);

        UpdateBuilder deleteAccountLink = new UpdateBuilder();
        deleteAccountLink.addDelete(usersGraphNode, personUriNode, FOAF.account.asNode(), accountUriNode);

        sparql.executeDeleteQuery(deleteAccountLink);
    }

    /**
     * update the Person with the URI uri with the given information
     * @return the updated PersonModel
     */
    public PersonModel update(URI uri, String firstName, String lastName, String email) throws Exception {
        PersonModel personModel = new PersonModel();
        personModel.setUri(uri);
        personModel.setFirstName(firstName);
        personModel.setLastName(lastName);

        sparql.startTransaction();
        try {
            if (email != null)
                personModel.setEmail(new InternetAddress(email));
            //save the potential link between the person and its account, because sparql.update() delete it
            AccountModel accountModel = getAccountFromPerson(uri);
            sparql.update(personModel);
            if (accountModel != null )
                setAccount(personModel.getUri(), accountModel.getUri());

            sparql.commitTransaction();
        } catch (Exception e){
            sparql.rollbackTransaction();
        }

        return personModel;
    }

    public ListWithPagination<PersonModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(PersonModel.FIRST_NAME_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(PersonModel.LAST_NAME_FIELD, stringPattern)
        );

        return sparql.searchWithPagination(
                sparql.getDefaultGraph(PersonModel.class),
                PersonModel.class,
                null,
                (SelectBuilder select) -> {
                    if (stringFilter != null) {
                        select.addFilter(stringFilter);
                    }
                },
                null,
                null,
                orderByList,
                page,
                pageSize
        );
    }

    /**
     * @param uri URI of the Person you are looking for
     * @return PersonModel corresponding to the URI uri
     */
    public PersonModel get(URI uri) throws Exception{
        return sparql.getByURI(PersonModel.class, uri, null);
    }

    /**
     * @param uri : URI of the Person to delete
     */
    public void delete(URI uri) throws Exception{
        sparql.delete(PersonModel.class, uri);

        //supression du lien foaf:account entre la foaf:Person et le foaf:OnlineAccount
        AccountModel account = getAccountFromPerson(uri);
        if ( account != null) {
            deleteAccountLink(uri, account.getUri());
        }
    }
}
