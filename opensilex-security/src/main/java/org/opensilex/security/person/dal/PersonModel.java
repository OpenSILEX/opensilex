//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.dal;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.uri.generation.ClassURIGenerator;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.Objects;

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
            ontology = SecurityOntology.class,
            property = "memberOf"
    )
    private String affiliation;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "phone"
    )
    private URI phoneNumber;

    /**
     * orcid of the person, set has the URI of the person at its creation. Can't be updated.
     */
    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasOrcid",
            ignoreUpdateIfNull = true
    )
    private URI orcid;
    public static final String ORCID_FIELD = "orcid";

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "account",
            ignoreUpdateIfNull = true
            //make easier the update of persons, but harder the deletion of the link between an account and a person
    )
    private AccountModel account;

    public static PersonModel fromDTO(PersonDTO personDTO, SPARQLService sparql) throws Exception {
        PersonModel person = new PersonModel();
        person.setUri(personDTO.getUri());
        person.setFirstName(personDTO.getFirstName());
        person.setLastName(personDTO.getLastName());
        person.setAffiliation(personDTO.getAffiliation());
        person.setOrcid(personDTO.getOrcid());
        if ( Objects.nonNull(personDTO.getPhoneNumber()) ){
            URI phone = new URI( "tel:" + personDTO.getPhoneNumber());
            person.setPhoneNumber(phone);
        }
        if (personDTO.getEmail() != null) {
            person.setEmail(new InternetAddress(personDTO.getEmail()));
        }

        AccountDAO accountDAO = new AccountDAO(sparql);
        AccountModel account = Objects.isNull(personDTO.getAccount()) ? null : accountDAO.get(personDTO.getAccount());
        person.setAccount(account);

        return person;
    }

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

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public URI getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(URI phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public URI getOrcid() {
        return orcid;
    }

    public void setOrcid(URI orcid) {
        this.orcid = orcid;
    }

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
