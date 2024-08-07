//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.person.api.ORCIDClient;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.Collections;
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
     * create a Person in dataBase with the given PersonModel information.
     * if orcid is given, it will be the uri of the person.
     *
     * @return the personModel of the created Person.
     */
    public PersonModel create(PersonModel person, ORCIDClient orcidClient) throws Exception {

        if (orcidIsNonNullAndWellFormed(person.getOrcid())) {
            requireOrcidIsUniqueAndExistsInOrcidAPI(person.getOrcid(), orcidClient);
            if ( ! sparql.uriExists((Node) null, person.getOrcid())) {
                person.setUri(person.getOrcid());
            }
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
    private boolean orcidIsNonNullAndWellFormed(URI orcid) throws BadRequestException {
        if (Objects.isNull(orcid)) {
            return false;
        }
        //commence par "https://orcid.org/" et est suivi de 3 séquences de 4 chiffres séparées par un tiret puis une séquence de 4 chiffres ou 3 chiffres et un X
        //exemples validés : https://orcid.org/0009-0006-6636-4714 ou https://orcid.org/0009-0006-6636-471X
        if ( ! orcid.toString().matches("https://orcid\\.org/(?:[\\d]{4}-){3}[\\d]{3}[\\dX]")) {
            throw new BadRequestException("orcid is not valid : "+ orcid);
        }

        return true;
    }

    /**
     * @param orcidID to check for, without the URI prefix https://orcid.org/
     * @throws BadRequestException if orcidID is not a well-formed orcidID
     */
    public void requireOrcidIDIsWellFormed(String orcidID){
        //commence par 3 séquences de 4 chiffres séparées par un tiret puis une séquence de 4 chiffres ou 3 chiffres et un X
        //exemples validés : 0009-0006-6636-4714 ou 0009-0006-6636-471X
        if ( ! orcidID.matches("^(?:[\\d]{4}-){3}[\\d]{3}[\\dX]$")) {
            throw new BadRequestException("orcidID is not valid : "+ orcidID);
        }
    }

    /**
     * @param orcid to check for
     * @throws ConflictException If Orcid is already taken
     * @throws NotFoundException If Orcid doesn't exist according to the ORCID API
     */
    private void requireOrcidIsUniqueAndExistsInOrcidAPI(URI orcid, ORCIDClient orcidClient) throws Exception, ConflictException{
        boolean orcidisAlreadytaken;

        orcidisAlreadytaken = sparql.existsByUniquePropertyValue(PersonModel.class, SecurityOntology.hasOrcid, orcid);

        if (orcidisAlreadytaken) {
            throw new ConflictException("Duplicated ORCID : " + orcid);
        }

        orcidClient.assertOrcidConnexionIsOk();
        if ( ! orcidClient.orcidExists(getIdPartOfAnOrcidUri(orcid)) ){
            throw new NotFoundException("orcid not found by the ORCID API : " + orcid);
        }
    }

    public String getIdPartOfAnOrcidUri(URI orcid){
        String[] orcidParts = orcid.toString().split("/");
        return orcidParts[orcidParts.length - 1];
    }

    /**
     * update the Person with the given information
     * orcid can't be updated
     *
     * @return the updated PersonModel
     */
    public PersonModel update(PersonDTO personDTO, ORCIDClient orcidClient) throws Exception {
        if (Objects.nonNull(personDTO.getOrcid())) {

            PersonModel originalPerson = get(personDTO.getUri());
            if (Objects.isNull(originalPerson.getOrcid())) {
                orcidIsNonNullAndWellFormed(personDTO.getOrcid());
                requireOrcidIsUniqueAndExistsInOrcidAPI(personDTO.getOrcid(), orcidClient);
            } else {
                boolean dtoOrcidIsTheSameAsOriginalOrcid = SPARQLDeserializers.compareURIs(originalPerson.getOrcid(), personDTO.getOrcid());
                if ( ! dtoOrcidIsTheSameAsOriginalOrcid) {
                    throw new BadRequestException("Orcid can't be updated : " + personDTO.getUri() + " already has an Orcid");
                }
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
                SPARQLQueryHelper.regexFilter(PersonModel.EMAIL_FIELD, stringPattern),
                SPARQLQueryHelper.regexStrFilter(PersonModel.ORCID_FIELD, stringPattern)
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
     *
     * @param uriFilter
     * @param lang
     * @return All Person models with minimal fields loaded
     * @throws Exception
     */
    public ListWithPagination<PersonModel> noProxySearch(List<URI> uriFilter, String lang) throws Exception {
        SparqlNoProxyFetcher<PersonModel> personsFetcher = new SparqlNoProxyFetcher<>(PersonModel.class, sparql);

        return sparql.searchWithPagination(
                sparql.getDefaultGraph(PersonModel.class),
                PersonModel.class,
                lang,
                (SelectBuilder select) -> {
                    if(!CollectionUtils.isEmpty(uriFilter)){
                        select.addFilter(SPARQLQueryHelper.inURIFilter(GroupUserProfileModel.URI_FIELD, uriFilter));
                    }
                },
                Collections.emptyMap(),
                (SPARQLResult result) -> personsFetcher.getInstance(result, lang),
                Collections.emptyList(),
                0,
                0
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
     */
    public List<PersonModel> getList(List<URI> uriList) throws Exception{
        return sparql.getListByURIs(PersonModel.class, uriList, null);
    }

}
