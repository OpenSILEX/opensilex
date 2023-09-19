//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.account.dal;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.OpenSilex;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "OnlineAccount",
        graph = AccountModel.GRAPH,
        prefix = "account"
)
public class AccountModel extends SPARQLResourceModel implements Principal, ClassURIGenerator<AccountModel> {
    public static final String GRAPH = "user";

    public static AccountModel getAnonymous() {
        AccountModel anonymous = new AccountModel();
        try {
            anonymous.setEmail(new InternetAddress("no@email.com"));
        } catch (AddressException ex) {
            // can't happend
        }
        anonymous.setUserProfiles(new ArrayList<>());
        anonymous.setAdmin(false);
        anonymous.setAnonymous(true);
        anonymous.setLocale(new Locale(OpenSilex.DEFAULT_LANGUAGE));

        return anonymous;
    }
    
    public static AccountModel getSystemUser() {
        AccountModel system = new AccountModel();
        try {
            system.setEmail(new InternetAddress("opensilex@gmail.com"));
        } catch (AddressException ex) {
            // can't happend
        }
        system.setUserProfiles(new ArrayList<>());
        system.setAdmin(true);
        system.setAnonymous(false);
        system.setLocale(new Locale(OpenSilex.DEFAULT_LANGUAGE));

        return system;
    }

    /**
     * convenient method used to get an AccountModel from non-complete information
     * @return the AccountModel instanced with given information
     */
    public static AccountModel buildAccountModel(URI uri,
                                                 InternetAddress email,
                                                 boolean admin,
                                                 String passwordHash,
                                                 String lang,
                                                 Boolean enable,
                                                 PersonModel holderOfTheAccount,
                                                 List<URI> favorites) {

        AccountModel accountModel = new AccountModel();
        accountModel.setUri(uri);
        accountModel.setEmail(email);
        accountModel.setAdmin(admin);
        accountModel.setLocale(new Locale(lang));
        accountModel.setLinkedPerson(holderOfTheAccount);
        accountModel.setFavorites(favorites);
        accountModel.setIsEnabled(enable);
        if (passwordHash != null) {
            accountModel.setPasswordHash(passwordHash);
        }
        if (enable != null) {
            accountModel.setIsEnabled(enable);
        }

        return accountModel;
    }

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "accountName",
            required = true,
            ignoreUpdateIfNull = true
    )
    private InternetAddress email;
    public static final String EMAIL_FIELD = "email";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasPasswordHash",
            ignoreUpdateIfNull = true
    )
    private String passwordHash;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "isAdmin",
            ignoreUpdateIfNull = true
    )
    private Boolean admin = Boolean.FALSE;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasLanguage",
            ignoreUpdateIfNull = true
    )
    private String language;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasUser",
            inverse = true,
            cascadeDelete = true,
            ignoreUpdateIfNull = true
    )
    private List<GroupUserProfileModel> userProfiles;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "topic_interest",
            ignoreUpdateIfNull = true
    )
    private List<URI> favorites;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "account",
            inverse = true
    )
    private PersonModel linkedPerson = null;
    public static final String LINKED_PERSON_FIELD = "linkedPerson";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "isEnabled",
            ignoreUpdateIfNull = true
    )
    private Boolean isEnabled = Boolean.TRUE;


    private String token;

    private boolean anonymous = false;

    public InternetAddress getEmail() {
        return email;
    }

    public void setEmail(InternetAddress email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getName() { return getEmail().toString(); }

    public List<GroupUserProfileModel> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(List<GroupUserProfileModel> userProfiles) {
        this.userProfiles = userProfiles;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Locale getLocale() {
        return new Locale(getLanguage());
    }

    public void setLocale(Locale locale) {
        setLanguage(locale.getLanguage());
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public PersonModel getLinkedPerson() {
        return linkedPerson;
    }

    public void setLinkedPerson(PersonModel linkedPerson) {
        this.linkedPerson = linkedPerson;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean enable) {
        if (Boolean.FALSE.equals(isAdmin())) {
            isEnabled = enable;
        } else {
            isEnabled = true;
        }
    }

    @Override
    public String[] getInstancePathSegments(AccountModel instance) {
        return new String[]{
                "account",
                instance.getEmail().toString()
        };
    }

    public List<URI> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<URI> favorites) {
        this.favorites = favorites;
    }
}
