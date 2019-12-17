//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.user.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.InternetAddress;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.security.dal.SecurityProfileModel;
import org.opensilex.rest.security.dal.SecurityProfileModelDAO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vincent
 */
public class UserDAO {

    private SPARQLService sparql;
    private AuthenticationService authentication;

    public UserDAO(SPARQLService sparql, AuthenticationService authentication) {
        this.sparql = sparql;
        this.authentication = authentication;
    }

    public UserModel getByEmail(InternetAddress email) throws Exception {
        return sparql.getByUniquePropertyValue(UserModel.class,
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
            String password
    ) throws Exception {
        UserModel user = new UserModel();
        user.setUri(uri);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAdmin(admin);

        if (password != null) {
            user.setPasswordHash(authentication.getPasswordHash(password));
        }

        sparql.create(user);

        return user;
    }

    public boolean authenticate(UserModel user, String password) throws Exception {
        if ((user != null && authentication.checkPassword(password, user.getPasswordHash()))) {
            List<String> accessList = getAccessList(user.getUri());
            authentication.generateToken(user, accessList);
            authentication.addUser(user, authentication.getExpireInMs());
            return true;
        }
        return false;
    }

    public boolean authenticate(UserModel user) throws Exception {
        List<String> accessList = getAccessList(user.getUri());
        authentication.generateToken(user, accessList);
        authentication.addUser(user, authentication.getExpireInMs());
        return true;
    }

    public boolean renewToken(UserModel user) throws Exception {
        authentication.renewToken(user);
        authentication.addUser(user, authentication.getExpireInMs());
        return true;
    }

    public boolean logout(UserModel user) {
        return (authentication.removeUser(user) != null);
    }

    public boolean userEmailexists(InternetAddress email) throws Exception {
        return sparql.existsByUniquePropertyValue(
                UserModel.class,
                FOAF.mbox,
                email
        );
    }

    public UserModel get(URI uri) throws Exception {
        return sparql.getByURI(UserModel.class, uri);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(UserModel.class, instanceURI);
    }

    public UserModel update(
            URI uri,
            InternetAddress email,
            String firstName,
            String lastName,
            boolean admin, String password
    ) throws Exception {
        UserModel user = new UserModel();
        user.setUri(uri);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAdmin(admin);

        if (password != null) {
            user.setPasswordHash(authentication.getPasswordHash(password));
        }

        sparql.update(user);
        
        return user;
    }

    public List<String> getAccessList(URI uri) throws Exception {
        SecurityProfileModelDAO profileDAO = new SecurityProfileModelDAO(sparql);
        List<SecurityProfileModel> userProfiles = profileDAO.getByUserURI(uri);

        List<String> accessList = new ArrayList<>();
        userProfiles.forEach((SecurityProfileModel profile) -> {
            accessList.addAll(profile.getAccessList());
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
}
