package org.opensilex.security.cli;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import javax.mail.internet.InternetAddress;

import java.util.List;

import static org.opensilex.security.SecurityModule.GUEST_OPENSILEX_ORG;

/**
 * @author rcolin
 */
public class UserCommandsTest {

    private static OpenSilexTestEnvironment openSilexTestEnv;

    @BeforeClass
    public static void beforeClass() throws Exception {
        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
    }

    @Test
    public void add() throws Exception {

        UserCommands command = new UserCommands();
        command.setOpenSilex(openSilexTestEnv.getOpenSilex());

        String mail = "test.test@opensilex.org";
        InternetAddress email = new InternetAddress(mail);

        AccountDAO accountDAO = new AccountDAO(openSilexTestEnv.getSparql());
        AccountModel user = accountDAO.getByEmail(new InternetAddress(mail));

        // default admin user created in test environment
        Assert.assertNull(user);

        int userCount = accountDAO.getCount();
        command.add(
                "firstName",
                "lastName",
                mail,
                "$2a$12$pGN0npUdY0HlfFI.EOGmEeorAwH5fdymVjEg3ya/ZSOAacv7nef3G",
                true,
                "en",
                null
        );
        Assert.assertEquals(userCount+1,accountDAO.getCount());

        user = accountDAO.getByEmail(email);
        Assert.assertNotNull(user);
        Assert.assertEquals(email,user.getEmail());
        Assert.assertEquals("firstName",user.getFirstName());
        Assert.assertEquals("lastName",user.getLastName());
        Assert.assertEquals(true,user.isAdmin());
        Assert.assertEquals("en",user.getLanguage());


        // test creation with same email -> no creation
        command.add(
                "firstName1",
                "lastName2",
                mail,
                "$2a$12$g.C2E/Q64RftrQp6SqNN5OaOUqVvx0Ir2SHyktayLnIzdVzs/qw9i",
                true,
                "en",
                null
        );
        Assert.assertEquals(userCount+1,accountDAO.getCount());
    }

    @Test
    public void testGuestAccount() throws Exception {

        UserCommands command = new UserCommands();
        command.setOpenSilex(openSilexTestEnv.getOpenSilex());

        // try to create guest account
        command.addGuest(null);

        // ensure that account is well created
        AccountDAO accountDAO = new AccountDAO(openSilexTestEnv.getSparql());
        AccountModel guestModel = accountDAO.getByEmail(new InternetAddress(GUEST_OPENSILEX_ORG));
        Assert.assertNotNull(guestModel);

        // check guest credentials
        ProfileDAO profileDAO = new ProfileDAO(openSilexTestEnv.getSparql());
        List<ProfileModel> guestProfiles = profileDAO.getByUserURI(guestModel.getUri());
        Assert.assertEquals(1, guestProfiles.size());

        // ensure that credentials are only for READ access
        guestProfiles.get(0).getCredentials().forEach(credential -> {
            Assert.assertTrue(credential.endsWith("access"));
            Assert.assertFalse(credential.contains("modification"));
            Assert.assertFalse(credential.endsWith("delete"));
        });
    }

    @AfterClass
    public static void shutdown() throws Exception {
        openSilexTestEnv.stopOpenSilex();
    }
}