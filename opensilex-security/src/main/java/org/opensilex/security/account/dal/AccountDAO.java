//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.account.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.OpenSilex;
import org.opensilex.security.account.ModuleWithNosqlEntityLinkedToAccount;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.*;

/**
 * AccountDAO is used to manipulate AccountModel and CRUD foaf:OnlineAccount data in the rdf database.
 *
 * @author vincent
 */
public final class AccountDAO {

    private final SPARQLService sparql;

    public AccountDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     * @param email the uniqueIdentifiant of an account, corresponding to foaf:accountName property in the rdf dataBase
     * @return the AccountModel corresponding to the email
     */
    public AccountModel getByEmail(InternetAddress email) throws Exception {
        return sparql.getByUniquePropertyValue(AccountModel.class,
                null,
                FOAF.accountName,
                email
        );
    }

    /**
     * save a new Account in the rdf Database
     *
     * @param uri   URI of the Account
     * @param email unique ID
     * @return the AccountModel corresponding to the Account created in the dataBase
     */
    public AccountModel create(
            URI uri,
            InternetAddress email,
            boolean admin,
            String passwordHash,
            String lang
    ) throws Exception {
        return create(uri,
                email,
                admin,
                passwordHash,
                lang,
                true,
                null);
    }

    /**
     * save a new Account in the rdf Database
     *
     * @param uri   URI of the Account
     * @param email unique ID
     * @return the AccountModel corresponding to the Account created in the dataBase
     */
    public AccountModel create(
            URI uri,
            InternetAddress email,
            boolean admin,
            String passwordHash,
            String lang,
            Boolean enable,
            PersonModel holderOfTheAccount
    ) throws Exception {

        AccountModel accountModel = AccountModel.buildAccountModel(uri, email, admin, passwordHash, lang, enable, holderOfTheAccount, Collections.emptyList());

        sparql.create(accountModel);

        return accountModel;
    }

    public AccountModel create(AccountModel account) throws Exception {
        sparql.create(account);
        return account;
    }

    /**
     * @return true if the email is already used by an Account in the dataBase
     */
    public boolean accountEmailExists(InternetAddress email) throws Exception {
        return sparql.existsByUniquePropertyValue(
                AccountModel.class,
                FOAF.accountName,
                email
        );
    }

    /**
     * @param uri URI of the Account you are looking for
     * @return AccountModel corresponding to the URI
     */
    public AccountModel get(URI uri) throws Exception {
        return sparql.getByURI(AccountModel.class, uri, null);
    }


    public List<AccountModel> getList(List<URI> uri) throws Exception {
        return sparql.getListByURIs(AccountModel.class, uri, null);
    }

    /**
     * Delete an account only if it is not linked to any other ressources in different databases.
     * Only one exception is made for the FOAF:account link between a Person and an Account.
     *
     * @param instanceURI uri of the Account to delete
     */
    public void delete(URI instanceURI, OpenSilex openSilex) throws Exception {
        Objects.requireNonNull(instanceURI);
        List<String> predicateUrisToExclude = new ArrayList<>();
        predicateUrisToExclude.add(FOAF.account.getURI());
        sparql.requireUriIsNotLinkedWithOtherRessourcesInRDF(instanceURI, predicateUrisToExclude);
        requireAccountUriIsNotLinkedWithOtherRessourcesInNosql(instanceURI, openSilex);

        sparql.delete(AccountModel.class, instanceURI);
    }

    /**
     * Itterate over all module that implement ModuleWithNosqlEntityLinkedToAccount interface and ask them if there are linked or not to accountUri.
     *
     * @throws ConflictException if at least one of those module is linked with accountUri.
     */
    private void requireAccountUriIsNotLinkedWithOtherRessourcesInNosql(URI accountUri, OpenSilex openSilex) throws ConflictException {
        boolean accountIsUsedInNosqlDatabase = openSilex.getModulesImplementingInterface(ModuleWithNosqlEntityLinkedToAccount.class)
                .stream()
                .anyMatch(
                        module -> module.accountIsLinkedWithANosqlEntity(accountUri)
                );
        if (accountIsUsedInNosqlDatabase) {
            throw new ConflictException("URI <" + accountUri + "> is linked with other ressources");
        }
    }

    /**
     * update the Account with the given information
     *
     * @return the updated Account
     */
    public AccountModel update(
            URI uri,
            InternetAddress email,
            boolean admin,
            String passwordHash,
            String lang,
            Boolean enable,
            PersonModel holderOfTheAccount,
            List<URI> favorites
    ) throws Exception {
        AccountModel accountModel = AccountModel.buildAccountModel(uri, email, admin, passwordHash, lang, enable, holderOfTheAccount, favorites);
        sparql.update(accountModel);

        return accountModel;
    }

    public List<String> getCredentialList(URI uri) throws Exception {
        ProfileDAO profileDAO = new ProfileDAO(sparql);
        List<ProfileModel> userProfiles = profileDAO.getByUserURI(uri);

        List<String> accessList = new ArrayList<>();
        userProfiles.forEach((ProfileModel profile) -> accessList.addAll(profile.getCredentials()));

        return accessList;
    }

    /**
     * @param stringPattern : pattern we will use to find a corresponding match with Account's email (ex .* to match every Account)
     * @return a paginatedList of AccountModel which correspond to the Pattern
     */
    public ListWithPagination<AccountModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(AccountModel.EMAIL_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(PersonModel.LAST_NAME_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(PersonModel.FIRST_NAME_FIELD, stringPattern)
        );

        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        WhereBuilder whereGraph = new WhereBuilder();
        whereGraph.addGraph(sparql.getDefaultGraph(PersonModel.class), SPARQLQueryHelper.makeVar(AccountModel.LINKED_PERSON_FIELD), FOAF.lastName.asNode(), SPARQLQueryHelper.makeVar(PersonModel.LAST_NAME_FIELD));
        whereGraph.addGraph(sparql.getDefaultGraph(PersonModel.class), SPARQLQueryHelper.makeVar(AccountModel.LINKED_PERSON_FIELD), FOAF.firstName.asNode(), SPARQLQueryHelper.makeVar(PersonModel.FIRST_NAME_FIELD));
        WhereBuilder whereOptional = new WhereBuilder();
        whereOptional.addOptional(whereGraph);
        customHandlerByFields.put(AccountModel.LINKED_PERSON_FIELD, whereOptional.getWhereHandler());

        return sparql.searchWithPagination(
                sparql.getDefaultGraph(AccountModel.class),
                AccountModel.class,
                null,
                (SelectBuilder select) -> {
                    if (stringFilter != null) {
                        select.addFilter(stringFilter);
                    }
                },
                customHandlerByFields,
                null,
                orderByList,
                page,
                pageSize
        );
    }

    /**
     * @return the number of Account saved in the database
     */
    public int getCount() throws Exception {
        return sparql.count(AccountModel.class);
    }

    /**
     * convenient method to get an AccountModel or create a new one if it doesn't exist yet
     *
     * @param accountModel the accountModel we want to get
     * @return the desired AccountModel after created it if necessary
     */
    public AccountModel getByEmailOrCreate(AccountModel accountModel, String defaultLang) throws Exception {
        AccountModel loadedAccount = getByEmail(accountModel.getEmail());

        if (loadedAccount == null) {
            loadedAccount = create(null, accountModel.getEmail(), false, accountModel.getPasswordHash(), defaultLang, null, accountModel.getLinkedPerson());
        }

        return loadedAccount;
    }

    public boolean accountExists(URI accountURI) throws SPARQLException {
        return sparql.uriExists(sparql.getDefaultGraph(AccountModel.class), accountURI);
    }
}
