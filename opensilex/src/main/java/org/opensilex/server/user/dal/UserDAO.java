//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.user.dal;

import java.net.URI;
import java.util.List;
import javax.mail.internet.InternetAddress;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.sparql.SPARQLQueryHelper;
import org.opensilex.sparql.SPARQLService;
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
            InternetAddress email,
            String firstName,
            String lastName,
            String password
    ) throws Exception {
        UserModel user = new UserModel();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (password != null) {
            user.setPasswordHash(authentication.getPasswordHash(password));
        }

        sparql.create(user);

        return user;
    }

    public boolean authenticate(UserModel user, String password) throws Exception {
        return (user != null && authentication.checkPassword(password, user.getPasswordHash()));
    }

    public boolean userEmailexists(InternetAddress email) throws Exception {
        return sparql.existsByUniquePropertyValue(
                UserModel.class,
                FOAF.mbox,
                email
        );
    }

    public UserModel getByURI(URI uri) throws Exception {
        return sparql.getByURI(UserModel.class, uri);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(UserModel.class, instanceURI);
    }

    public UserModel update(UserModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public ListWithPagination<UserModel> find(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

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
