package org.opensilex.security.account.dal;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.account.ModuleWithNosqlEntityLinkedToAccount;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.person.api.ORCIDClient;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class AccountDAOTest extends AbstractSecurityIntegrationTest {
    private AccountDAO accountDAO;
    private GroupDAO groupDAO;
    private ProfileDAO profileDAO;

    @Before
    public void setUpDAO(){
        accountDAO = accountDAO == null ? new AccountDAO(getSparqlService()) : accountDAO;
        groupDAO = groupDAO == null ? new GroupDAO(getSparqlService()) : groupDAO;
        profileDAO = profileDAO == null ? new ProfileDAO(getSparqlService()) : profileDAO;
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> models = new ArrayList<>();
        models.add(AccountModel.class);
        return models;
    }

    private AccountModel getAccountModel() throws AddressException {
        AccountModel accountModel = new AccountModel();
        accountModel.setAdmin(false);
        accountModel.setEmail( new InternetAddress("account1@opensilex.org") );
        accountModel.setPasswordHash("1234");
        accountModel.setLanguage(OpenSilex.DEFAULT_LANGUAGE);
        return accountModel;
    }

    @Test
    public void deleteAccount() throws Exception {
        AccountModel accountModel = accountDAO.create(getAccountModel());

        accountDAO.delete(accountModel.getUri(), getOpensilex());
        assertFalse( accountDAO.accountExists(accountModel.getUri()) );
    }

    @Test
    public void deleteThrowsErrorIfAccountIsUsedInTheRDFDataBase_publisher_predicate_group() throws Exception {
        AccountModel accountModel = accountDAO.create(getAccountModel());

        GroupModel groupModel = new GroupModel();
        groupModel.setDescription("description");
        groupModel.setName("group");
        groupModel.setUserProfiles(new ArrayList<>());

        groupModel.setPublisher(accountModel.getUri());
        groupModel = groupDAO.create(groupModel);

        assertThrows(ConflictException.class, () -> accountDAO.delete(accountModel.getUri(), getOpensilex()) );
        assertNotNull( groupDAO.get(groupModel.getUri()) );
        assertTrue( accountDAO.accountExists(accountModel.getUri()) );
    }

    @Test
    public void deleteThrowsErrorIfAccountIsUsedInTheRDFDataBase_publisher_predicate_profile() throws Exception {
        AccountModel accountModel = accountDAO.create(getAccountModel());

        ProfileModel profileModel = profileDAO.create(null, "profile-name", new ArrayList<>(), accountModel.getUri());

        assertThrows(ConflictException.class, () -> accountDAO.delete(accountModel.getUri(), getOpensilex()) );
        assertNotNull( profileDAO.get(profileModel.getUri()) );
        assertTrue( accountDAO.accountExists(accountModel.getUri()) );
    }

    @Test
    public void deleteThrowsErrorIfAccountIsUsedInTheRDFDataBase_hasUser_predicate() throws Exception {
        AccountModel accountModel = accountDAO.create(getAccountModel());

        //create group model without publisher but with a GroupUserProfile containing the user
        ProfileModel profileModel = profileDAO.create(null, "profile-name", new ArrayList<>(), accountModel.getUri());
        List<GroupUserProfileModel> userProfiles = new ArrayList<>();
        GroupUserProfileModel groupWithAccount = new GroupUserProfileModel();
        groupWithAccount.setProfile(profileModel);
        groupWithAccount.setUser(accountModel);
        GroupModel groupModel = new GroupModel();
        groupModel.setDescription("description");
        groupModel.setName("group");

        userProfiles.add(groupWithAccount);
        groupModel.setUserProfiles(userProfiles);
        groupModel = groupDAO.create(groupModel);

        assertThrows(ConflictException.class, () -> accountDAO.delete(accountModel.getUri(), getOpensilex()) );
        assertNotNull( groupDAO.get(groupModel.getUri()) );
        assertTrue( accountDAO.accountExists(accountModel.getUri()) );
    }

    @Test
    public void deleteIsPossibleIfAccountIsOnlyLinkedToAPerson() throws Exception {
        PersonDAO personDAO = new PersonDAO(getSparqlService());
        PersonModel personModel = new PersonModel();
        personModel.setFirstName("test");
        personModel.setLastName("test");
        personModel = personDAO.create(personModel, new ORCIDClient());

        AccountModel accountModel = getAccountModel();
        accountModel.setLinkedPerson(personModel);
        accountModel = accountDAO.create(accountModel);

        accountDAO.delete(accountModel.getUri(), getOpensilex());
        assertFalse( accountDAO.accountExists(accountModel.getUri()) );
        assertNull( personDAO.get(personModel.getUri()).getAccount() );
    }

    // purpose is only to test DAO. We don't want to test #accountIsLinkedWithANosqlEntity methods of ModuleWithNosqlEntityLinkedToAccount implementations.
    @Test
    public void deleteThrowsErrorIfAccountIsUsedInTheMongoDataBase() throws Exception {
        //creating mock (fake) implementation of ModuleWithNosqlEntityLinkedToAccount that will always return true to #accountIsLinkedWithANosqlEntity method call.
        ModuleWithNosqlEntityLinkedToAccount mockedModule = mock(ModuleWithNosqlEntityLinkedToAccount.class);
        when(mockedModule.accountIsLinkedWithANosqlEntity(any())).thenReturn(true);

        //creating mock Opensilex that will return our mocked implementation when calling #getModulesImplementingInterface method.
        OpenSilex mockedOpensilex = mock(OpenSilex.class);
        when(mockedOpensilex.getModulesImplementingInterface(ModuleWithNosqlEntityLinkedToAccount.class)).thenReturn(List.of(mockedModule));

        AccountModel accountModel = accountDAO.create(getAccountModel());

        assertThrows(ConflictException.class, () -> accountDAO.delete(accountModel.getUri(), mockedOpensilex) );
        assertTrue( accountDAO.accountExists(accountModel.getUri()) );
    }
}
