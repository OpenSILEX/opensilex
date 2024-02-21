package org.opensilex.migration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.account.api.AccountAPI;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.person.api.PersonAPI;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.user.api.UserAPI;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL;

public class AddAccountCredentialsToProfilWithUserCredentialTest extends AbstractSecurityIntegrationTest {

    private static ProfileDAO profileDAO;
    private static AddAccountCredentialsToProfilWithUserCredential migration;
    private static int uriCount = 0;
    private static URI adminURI;

    @BeforeClass
    public static void setUpLists() throws Exception {
        OpenSilex openSilex = getOpensilex();
        SPARQLService sparql = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        migration = new AddAccountCredentialsToProfilWithUserCredential();
        migration.setOpensilex(opensilex);
        profileDAO = new ProfileDAO(sparql);
        createAdmin();
        adminURI = new AccountDAO(sparql).getByEmail(new InternetAddress(DEFAULT_SUPER_ADMIN_EMAIL)).getUri();
    }

    private static List<String> getUserCredentials() {
        List<String> userCredentials = new ArrayList<>();
        userCredentials.add(UserAPI.CREDENTIAL_USER_MODIFICATION_ID);
        userCredentials.add(AddAccountCredentialsToProfilWithUserCredential.USER_ACCESS_CREDENTIAL);
        return userCredentials;
    }

    private static List<String> getAccountCredentials() {
        List<String> accountCredentials = new ArrayList<>();
        accountCredentials.add(AccountAPI.CREDENTIAL_ACCOUNT_MODIFICATION_ID);
        accountCredentials.add(AddAccountCredentialsToProfilWithUserCredential.ACCOUNT_ACCESS_CREDENTIAL);
        return accountCredentials;
    }

    private URI getNonExistingUri() throws URISyntaxException {
        uriCount++;
        return new URI("http://profilUri/test/" + uriCount);
    }

    @Test
    public void migrationAddBothCredentials() throws Exception {
        URI profilUri = getNonExistingUri();
        profileDAO.create(profilUri, "withUsers", getUserCredentials(), adminURI);
        migration.execute();

        List<String> expectedList = getUserCredentials();
        expectedList.addAll(getAccountCredentials());
        compareLists(expectedList, profileDAO.get(profilUri).getCredentials() );
    }

    @Test
    public void migrationDontChangeOtherCredantials() throws Exception {
        URI profilUri = getNonExistingUri();
        List<String> baseCredentialList = getUserCredentials();
        baseCredentialList.add(PersonAPI.CREDENTIAL_PERSON_MODIFICATION_ID);
        baseCredentialList.add(GermplasmAPI.CREDENTIAL_GERMPLASM_DELETE_ID);
        profileDAO.create(profilUri, "withUsers", baseCredentialList, adminURI);
        migration.execute();

        List<String> expectedList = baseCredentialList;
        expectedList.addAll(getAccountCredentials());
        compareLists(expectedList, profileDAO.get(profilUri).getCredentials() );
    }

    @Test
    public void migrateOnlyOneOfBothCredentials() throws Exception {
        URI firstURI = getNonExistingUri();
        List<String> firstBaseCredential = new ArrayList<>();
        firstBaseCredential.add(getUserCredentials().get(0));
        profileDAO.create(firstURI, "", firstBaseCredential, adminURI);
        List<String> firstExpected = new ArrayList<>(firstBaseCredential);
        firstExpected.add(getAccountCredentials().get(0));

        URI secondUri = getNonExistingUri();
        List<String> secondBaseCredential = new ArrayList<>();
        secondBaseCredential.add(getUserCredentials().get(1));
        profileDAO.create(secondUri, "", secondBaseCredential, adminURI);
        List<String> secondExpected = new ArrayList<>(secondBaseCredential);
        secondExpected.add(getAccountCredentials().get(1));

        migration.execute();

        compareLists(firstExpected, profileDAO.get(firstURI).getCredentials());
        compareLists(secondExpected, profileDAO.get(secondUri).getCredentials());
    }

    private static void compareLists(List<String> expectedList, List<String> profileList) {
        assertTrue(expectedList.containsAll(profileList));
        assertEquals(expectedList.size(), profileList.size());
    }

}