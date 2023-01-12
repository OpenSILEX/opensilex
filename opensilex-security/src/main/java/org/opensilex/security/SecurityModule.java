//******************************************************************************
//                          SecurityModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security;

import org.apache.jena.riot.Lang;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.authentication.dal.AuthenticationDAO;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.authentication.injection.CurrentUserFactory;
import org.opensilex.security.authentication.injection.CurrentUserResolver;
import org.opensilex.security.extensions.LoginExtension;
import org.opensilex.security.ontology.OesoSecurity;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opensilex.security.group.api.GroupAPI;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.api.ProfileAPI;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.user.api.UserAPI;

public class SecurityModule extends OpenSilexModule implements APIExtension, LoginExtension, SPARQLExtension {

    public final static String REST_SECURITY_API_ID = "Security";

    public final static String REST_AUTHENTICATION_API_ID = "Authentication";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityModule.class);

    @Override
    public Class<?> getConfigClass() {
        return SecurityConfig.class;
    }

    @Override
    public String getConfigId() {
        return "security";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("io.swagger.jaxrs.listing");
        list.add("org.opensilex.security.authentication");
        list.add("org.opensilex.security.authentication.filters");
        list.add("org.opensilex.security.authentication.injection");

        return list;
    }

    @Override
    public void setup() throws Exception {
        SPARQLService.addPrefix(SecurityOntology.PREFIX, SecurityOntology.NAMESPACE);
    }

    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Create default profile");
        createDefaultProfile(reset);
    }

    private final static String DEFAULT_PROFILE_URI = "http://www.opensilex.org/profiles/default-profile";
    private final static String DEFAULT_PROFILE_NAME = "Default profile";

    public final static String GUEST_PROFILE_URI = "http://www.opensilex.org/profiles/guest-profile";
    private final static String GUEST_PROFILE_NAME = "Guest profile";
    
    public final static String GUEST_GROUP_URI = "http://www.opensilex.org/groups/guest";
    private final static String GUEST_GROUP_NAME = "Guest group";
    private final static String GUEST_GROUP_DESCRIPTION = "Manage guest group persons";

    
    public void createDefaultProfile(boolean reset) throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        AuthenticationDAO securityDAO = new AuthenticationDAO(sparql);

        ProfileModel profile = new ProfileModel();
        profile.setUri(new URI(DEFAULT_PROFILE_URI));
        profile.setName(DEFAULT_PROFILE_NAME);
        profile.setCredentials(new ArrayList<>(securityDAO.getCredentialsIdList()));
        sparql.create(profile);
        factory.dispose(sparql);
    }

    public void createGuestProfile(boolean reset) throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        AuthenticationDAO securityDAO = new AuthenticationDAO(sparql);

        ProfileModel profile = new ProfileModel();
        profile.setUri(new URI(GUEST_PROFILE_URI));
        profile.setName(GUEST_PROFILE_NAME);
        ArrayList<String> credentialsIdList = new ArrayList<>(securityDAO.getCredentialsIdList());
        // remove credentials with delete
        credentialsIdList.removeIf(n -> (n.contains("delete")));
        credentialsIdList.removeIf(n -> (n.contains("modification")));
        profile.setCredentials(credentialsIdList);

        sparql.create(profile);
        factory.dispose(sparql);
    }

    @Override
    public void check() throws Exception {
        LOGGER.info("Check User existence");
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        AccountDAO accountDAO = new AccountDAO(sparql);
        int userCount = accountDAO.getCount();
        factory.dispose(sparql);
        if (userCount == 0) {
            LOGGER.warn("/!\\ Caution, you don't have any user registered in OpenSilex");
            LOGGER.warn("/!\\ You probably should add one with command `opensilex user add ...` (use --help flag for more information)");
        }
    }

    public void createDefaultGuestGroupUserProfile() throws Exception {
        OpenSilex opensilex = getOpenSilex();
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        try {
            AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
            createDefaultGuestProfile(sparql);
            createDefaultGuestUser(sparql, authentication);
            createDefaultGuestGroup(sparql); 
        } finally {
            factory.dispose(sparql);
        }
    }

    public void createDefaultGuestUser(SPARQLService sparql, AuthenticationService authentication) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);
        InternetAddress email = new InternetAddress("guest@opensilex.org");

        if (!accountDAO.accountEmailExists(email)) {
            accountDAO.create(null, email, false, authentication.getPasswordHash("guest"), "en");
        }
    }

    public void createDefaultGuestProfile(SPARQLService sparql) throws Exception {
        if (!sparql.uriExists(ProfileModel.class, new URI(SecurityModule.GUEST_PROFILE_URI))) {
            createGuestProfile(false);
        }
    }

    public static void createDefaultGuestGroup(SPARQLService sparql ) throws Exception {
        if (!sparql.uriExists(GroupModel.class, new URI(SecurityModule.GUEST_GROUP_URI))) {
            GroupDAO groupDAO = new GroupDAO(sparql);
            GroupModel groupModel = new GroupModel();
            groupModel.setUri(new URI(GUEST_GROUP_URI));
            groupModel.setName(GUEST_GROUP_NAME);
            groupModel.setDescription(GUEST_GROUP_DESCRIPTION);

            GroupUserProfileModel groupUserProfileModel = new GroupUserProfileModel();
            ProfileDAO profileDAO = new ProfileDAO(sparql);
            ProfileModel guestProfilModel = profileDAO.get(new URI(GUEST_PROFILE_URI));
            groupUserProfileModel.setProfile(guestProfilModel);

            InternetAddress email = new InternetAddress("guest@opensilex.org");
            AccountDAO accountDAO = new AccountDAO(sparql);
            AccountModel guestUserModel = accountDAO.getByEmail(email);
            groupUserProfileModel.setUser(guestUserModel);

            groupModel.setUserProfiles(new ArrayList<>(Collections.singletonList(groupUserProfileModel)));
            groupDAO.create(groupModel); 
        }
    }

    public void createDefaultSuperAdmin() throws Exception {
        OpenSilex opensilex = getOpenSilex();
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        try {
            AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

            createDefaultSuperAdmin(sparql, authentication);
        } finally {
            factory.dispose(sparql);
        }
    }

    public static void createDefaultSuperAdmin(SPARQLService sparql, AuthenticationService authentication) throws Exception {
        AccountDAO accountDAO = new AccountDAO(sparql);
        InternetAddress email = new InternetAddress("admin@opensilex.org");

        if (!accountDAO.accountEmailExists(email)) {
            accountDAO.create(null, email, true, authentication.getPasswordHash("admin"), "en");
        }
    }

    @Override
    public void bindServices(AbstractBinder binder) {
        binder.bindFactory(CurrentUserFactory.class).to(AccountModel.class)
                .proxy(true).proxyForSameScope(false).in(RequestScoped.class);

        binder.bind(CurrentUserResolver.class).to(new TypeLiteral<InjectionResolver<CurrentUser>>() {
        }).in(Singleton.class);
    }

    @Override
    public void inMemoryInitialization() throws Exception {
        createDefaultSuperAdmin();
    }

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {

        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();

        list.add(new OntologyFileDefinition(
                OesoSecurity.NS,
                "ontologies/os-sec.owl",
                Lang.RDFXML,
                OesoSecurity.PREFIX
        ));

        list.add(new OntologyFileDefinition(
                FOAF.NAMESPACE,
                "ontologies/foaf.rdf",
                Lang.RDFXML,
                "foaf",
                FOAF.NAMESPACE
        ));

        list.add(new OntologyFileDefinition(
                VCARD4.NAMESPACE,
                "ontologies/vcard.ttl",
                Lang.TTL,
                "vcard"
        ));

        list.add(new OntologyFileDefinition(
                ORG.NAMESPACE,
                "ontologies/org.ttl",
                Lang.TTL,
                "org"
        ));

        return list;
    }
}
