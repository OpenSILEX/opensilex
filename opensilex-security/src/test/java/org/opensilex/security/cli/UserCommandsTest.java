package org.opensilex.security.cli;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.security.user.dal.UserDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import javax.mail.internet.InternetAddress;

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

        UserDAO userDAO = new UserDAO(openSilexTestEnv.getSparql());

        // default admin user created in test environment
        Assert.assertEquals(0,userDAO.getCount());

        command.add(
                "firstName",
                "lastName",
                mail,
                "$2a$12$pGN0npUdY0HlfFI.EOGmEeorAwH5fdymVjEg3ya/ZSOAacv7nef3G",
                true,
                "en",
                null
        );
        Assert.assertEquals(1,userDAO.getCount());

        UserModel user = userDAO.getByEmail(email);
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
        Assert.assertEquals(1,userDAO.getCount());

    }

    @AfterClass
    public static void shutdown() throws Exception {
        openSilexTestEnv.stopOpenSilex();
    }
}