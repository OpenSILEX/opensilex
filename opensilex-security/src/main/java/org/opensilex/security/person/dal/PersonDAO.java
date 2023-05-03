//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;

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
            String email,
            AccountModel account
    ) throws Exception {
        PersonModel person = new PersonModel();
        person.setUri(uri);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        if (email != null) {
            person.setEmail(new InternetAddress(email));
        }
        person.setAccount(account);

        sparql.create(person);

        return person;
    }

    /** @return  the Person linked to an account if this person exists, null otherwise. */
    public PersonModel getPersonFromAccount(URI accountURI) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);
        AccountModel accountModel = accountDAO.get(accountURI);
        if (accountModel == null){
            return null;
        }
        return accountModel.getHolderOfTheAccount();
    }

    /**
     * update the Person with the given information
     * @return the updated PersonModel
     */
    public PersonModel update(URI uri, String firstName, String lastName, String email, AccountModel account) throws Exception {
        PersonModel personModel = new PersonModel();
        personModel.setUri(uri);
        personModel.setFirstName(firstName);
        personModel.setLastName(lastName);
        personModel.setAccount(account);

        sparql.startTransaction();
        try {
            if (email != null)
                personModel.setEmail(new InternetAddress(email));

            sparql.update(personModel);

            sparql.commitTransaction();
        } catch (SPARQLException e){
            sparql.rollbackTransaction();
        }

        return personModel;
    }

    public ListWithPagination<PersonModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return search(stringPattern, orderByList, page, pageSize, null);
    }

    public ListWithPagination<PersonModel> searchPersonsWithoutAccount(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        WhereBuilder where = new WhereBuilder();
        where.addWhere(SPARQLQueryHelper.makeVar("uri"), FOAF.account.asNode(), SPARQLQueryHelper.makeVar(AccountModel.class.getSimpleName()));
        WhereHandler whereHasNoAccount = where.getWhereHandler();
        return search(stringPattern, orderByList, page, pageSize, new E_NotExists(whereHasNoAccount.getElement()));
    }

    private ListWithPagination<PersonModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize, Expr whereConditions) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(PersonModel.FIRST_NAME_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(PersonModel.LAST_NAME_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(PersonModel.EMAIL_FIELD, stringPattern)
        );

        return sparql.searchWithPagination(
                sparql.getDefaultGraph(PersonModel.class),
                PersonModel.class,
                null,
                (SelectBuilder select) -> {
                    if (stringFilter != null) {
                        select.addFilter(stringFilter);
                    }
                    if (Objects.nonNull(whereConditions) ) {
                        select.addFilter(whereConditions);
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
    }

    public List<PersonModel> getList(List<URI> uri) throws Exception {
        return sparql.getListByURIs(PersonModel.class, uri, null);
    }
}
