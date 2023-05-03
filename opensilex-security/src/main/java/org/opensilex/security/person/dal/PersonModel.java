//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.dal;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import javax.mail.internet.InternetAddress;

/**
 *  Person model is used to stock data representing someone.
 * @author Yvan Roux
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "Person",
        graph = PersonModel.GRAPH,
        prefix = "person"
)
public class PersonModel extends SPARQLResourceModel implements ClassURIGenerator<PersonModel> {
    public static final String GRAPH = "user";

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "firstName",
            required = true
    )
    private String firstName;
    public static final String FIRST_NAME_FIELD = "firstName";
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "lastName",
            required = true
    )
    private String lastName;
    public static final String LAST_NAME_FIELD = "lastName";

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "mbox"
    )
    private InternetAddress email;
    public static final String EMAIL_FIELD = "email";

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "account",
            ignoreUpdateIfNull = true
            //make easier the update of persons, but harder the deletion of the link between an account and a person
    )
    private AccountModel account;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public InternetAddress getEmail() { return email; }

    public void setEmail(InternetAddress email) { this.email = email; }

    public AccountModel getAccount() { return account; }

    public void setAccount(AccountModel account) { this.account = account; }

    @Override
    public String[] getInstancePathSegments(PersonModel instance) {
        return new String[]{
                "person",
                instance.getFirstName(),
                instance.getLastName()
        };
    }
}
