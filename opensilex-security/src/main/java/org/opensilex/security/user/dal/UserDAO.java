//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.mail.internet.InternetAddress;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vincent
 */
public final class UserDAO {

    private final SPARQLService sparql;

    public UserDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public UserModel getByEmail(InternetAddress email) throws Exception {
        return sparql.getByUniquePropertyValue(UserModel.class,
                null,
                FOAF.mbox,
                email
        );
    }

    public UserModel create(
            URI uri,
            InternetAddress email,
            String firstName,
            String lastName,
            boolean admin,
            String passwordHash,
            String lang
    ) throws Exception {
        UserModel user = new UserModel();
        user.setUri(uri);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAdmin(admin);
        user.setLocale(new Locale(lang));
        if (passwordHash != null) {
            user.setPasswordHash(passwordHash);
        }

        sparql.create(user);

        return user;
    }

    public boolean userEmailexists(InternetAddress email) throws Exception {
        return sparql.existsByUniquePropertyValue(
                UserModel.class,
                FOAF.mbox,
                email
        );
    }

    public UserModel get(URI uri) throws Exception {
        return sparql.getByURI(UserModel.class, uri, null);
    }

    public List<UserModel> getList(List<URI> uri) throws Exception {
        return sparql.getListByURIs(UserModel.class, uri, null);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(UserModel.class, instanceURI);
    }

    public UserModel update(
            URI uri,
            InternetAddress email,
            String firstName,
            String lastName,
            boolean admin,
            String passwordHash,
            String lang
    ) throws Exception {
        UserModel user = new UserModel();
        user.setUri(uri);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAdmin(admin);
        user.setLocale(new Locale(lang));

        if (passwordHash != null) {
            user.setPasswordHash(passwordHash);
        }

        sparql.update(user);

        return user;
    }

    public List<String> getCredentialList(URI uri) throws Exception {
        ProfileDAO profileDAO = new ProfileDAO(sparql);
        List<ProfileModel> userProfiles = profileDAO.getByUserURI(uri);

        List<String> accessList = new ArrayList<>();
        userProfiles.forEach((ProfileModel profile) -> {
            accessList.addAll(profile.getCredentials());
        });

        return accessList;
    }

    public ListWithPagination<UserModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(UserModel.FIRST_NAME_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(UserModel.LAST_NAME_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(UserModel.EMAIL_FIELD, stringPattern)
        );

        return sparql.searchWithPagination(
                UserModel.class,
                null,
                (SelectBuilder select) -> {
                    if (stringFilter != null) {
                        select.addFilter(stringFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }

    @Deprecated
    public ListWithPagination<UserModel> search(String email, String firstName, String familyName, int page, int pageSize) throws Exception {
        return sparql.searchWithPagination(
                UserModel.class,
                null,
                (SelectBuilder select) -> {
                    if (firstName != null) {
                        select.addFilter(SPARQLQueryHelper.eq(UserModel.FIRST_NAME_FIELD, firstName));
                    }
                    if (firstName != null) {
                        select.addFilter(SPARQLQueryHelper.eq(UserModel.LAST_NAME_FIELD, familyName));
                    }
                    if (firstName != null) {
                        select.addFilter(SPARQLQueryHelper.eq(UserModel.EMAIL_FIELD, email));
                    }
                },
                null,
                page,
                pageSize
        );
    }

    public int getCount() throws Exception {
        return sparql.count(UserModel.class);
    }

    public UserModel getByEmailOrCreate(UserModel user, String defaultLang) throws Exception {
        UserModel loadedUser = getByEmail(user.getEmail());

        if (loadedUser == null) {
            loadedUser = create(null, user.getEmail(), user.getFirstName(), user.getLastName(), false, null, defaultLang);
        }

        return loadedUser;
    }

}
