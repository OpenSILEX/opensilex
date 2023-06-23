//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * PersonDAO is used to manipulate PersonModel and CRUD persons data in the rdf database.
 *
 * @author Yvan Roux
 */
public class PersonDAO {

    private final SPARQLService sparql;

    public PersonDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     * create a Person in dataBase with the given PersonDTO information.
     * if orcid is given, it will be the uri of the person.
     *
     * @return the personModel of the created Person.
     */
    public PersonModel create(PersonDTO personDTO) throws Exception {
        PersonModel person = PersonModel.fromDTO(personDTO, sparql);

        if (checkOrcidIsNonNullAndWellFormed(person.getOrcid())) {
            person.setUri(person.getOrcid());
        }

        if (person.getUri() != null && sparql.uriExists((Node) null, person.getUri())) {
            throw new ConflictException("Duplicated URI: " + person.getUri());
        }

        sparql.create(person);

        return person;
    }

    /**
     * @param orcid to check for
     * @return false if ORCID is null, true otherwise
     * @throws BadRequestException if orcid is not a well-formed URL
     */
    private boolean checkOrcidIsNonNullAndWellFormed(URI orcid) throws BadRequestException, ConflictException, SPARQLException {
        if (Objects.isNull(orcid)) {
            return false;
        }
        if ( ! orcid.toString().matches("https://orcid\\.org/(?:[\\d]{4}-){3}[\\d]{3}[\\dX]")) {
            throw new BadRequestException("orcid is not valid");
        }
        if (sparql.uriExists((Node) null, orcid)) {
            throw new ConflictException("Duplicated ORCID: " + orcid);
        }

        return true;
    }

    /**
     * @return the Person linked to an account if this person exists, null otherwise.
     */
    public PersonModel getPersonFromAccount(URI accountURI) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);
        AccountModel accountModel = accountDAO.get(accountURI);
        if (accountModel == null) {
            return null;
        }
        return accountModel.getHolderOfTheAccount();
    }

    /**
     * update the Person with the given information
     * orcid can't be updated
     *
     * @return the updated PersonModel
     */
    public PersonModel update(PersonDTO personDTO) throws Exception {
        if (Objects.nonNull(personDTO.getOrcid())) {

            checkOrcidIsNonNullAndWellFormed(personDTO.getOrcid());

            PersonModel originalPerson = get(personDTO.getUri());
            if (Objects.nonNull(originalPerson.getOrcid())) {
                throw new BadRequestException("Orcid can't be updated : " + personDTO.getUri() + " already has an Orcid");
            }
        }

        PersonModel personModel = PersonModel.fromDTO(personDTO, sparql);

        sparql.update(personModel);

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
                    if (Objects.nonNull(whereConditions)) {
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
    public PersonModel get(URI uri) throws Exception {
        return sparql.getByURI(PersonModel.class, uri, null);
    }

    /**
     * @param uriList List of the URIs of the Persons you are looking for
     * @return List<PersonModel> corresponding to the URIs in uriList
     * @throws Exception
     */
    public List<PersonModel> getList(List<URI> uriList) throws Exception{
        return sparql.getListByURIs(PersonModel.class, uriList, null);
    }

    /**
     * @param uri : URI of the Person to delete
     */
    public void delete(URI uri) throws Exception {
        sparql.delete(PersonModel.class, uri);
    }
}
