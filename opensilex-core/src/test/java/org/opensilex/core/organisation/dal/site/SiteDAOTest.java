package org.opensilex.core.organisation.dal.site;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.organisation.bll.SiteLogic;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


/**
 * @author Brice MAUSSANG
 */
public class SiteDAOTest extends AbstractMongoIntegrationTest {

    private static OpenSilexTestEnvironment openSilexTestEnv;
    private static AccountModel user;
    private static OrganizationModel organization;
    private static FacilityModel facilityA, facilityB;
    private static SiteModel siteA, siteB, siteC;
    private static SiteDAO dao;
    private static SiteLogic logic;

    private static SiteModel getModel(int i) {
        SiteModel model = new SiteModel();
        model.setName("site_dao_test_" + i);

        return model;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {

        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
        SPARQLService sparql = openSilexTestEnv.getSparql();

        // create organization
        organization = new OrganizationModel();
        organization.setUri(URI.create("test:infra"));
        sparql.create(OrganizationModel.class, Arrays.asList(organization));

        // create facilities
        facilityA = new FacilityModel();
        facilityA.setUri(URI.create("test:facilityA"));
        facilityB = new FacilityModel();
        facilityB.setUri(URI.create("test:facilityB"));
        sparql.create(FacilityModel.class, Arrays.asList(facilityA, facilityB));

        // create user
        user = new AccountModel();
        user.setUri(URI.create("test:user_test"));
        user.setLanguage("en");
        user.setAdmin(true);

        // create sites
        siteA = getModel(1);
        siteA.setOrganizations(Arrays.asList(organization));
        siteA.setFacilities(Arrays.asList(facilityA));
        siteB = getModel(2);
        siteB.setOrganizations(Arrays.asList(organization));
        siteB.setFacilities(Arrays.asList(facilityB));
        siteC = getModel(3);
        siteC.setOrganizations(Arrays.asList(organization));
        siteC.setFacilities(Arrays.asList(facilityA, facilityB));
        sparql.create(SiteModel.class, Arrays.asList(siteA, siteB, siteC));

        MongoDBServiceV2 mongo = openSilexTestEnv.getOpenSilex().getServiceInstance(MongoDBServiceV2.DEFAULT_SERVICE, MongoDBServiceV2.class);
        logic = new SiteLogic(sparql, mongo);
    }

    @Test
    public void testSearchAll() throws Exception {
        SiteSearchFilter filter = new SiteSearchFilter();
        filter.setLang("en");
        filter.setUser(user);
        filter.setUserOrganizations(Arrays.asList(organization.getUri()));
        filter.setSkipUserOrganizationFetch(true);

        List<SiteModel> objects = logic.search(filter).getList();

        assertNotNull(objects);
        assertEquals(3, objects.size());

        assertModelEquals(objects.get(0), siteA);
        assertModelEquals(objects.get(1), siteB);
        assertModelEquals(objects.get(2), siteC);
    }

    @Test
    public void testGetByUri() throws Exception {
        SiteModel site = logic.get(siteA.getUri(), user);

        assertModelEquals(site, siteA);
    }

    @Test
    public void testGetListByUris() throws Exception {
        List<URI> uris = Arrays.asList(siteA.getUri(), siteB.getUri(), siteC.getUri());
        List<SiteModel> sites = logic.getList(uris, user);

        assertModelEquals(sites.get(0), siteA);
        assertModelEquals(sites.get(1), siteB);
        assertModelEquals(sites.get(2), siteC);
    }

    @Test
    public void testGetByFacility() throws Exception {
        List<SiteModel> sites = logic.getByFacility(facilityA.getUri(), user);

        assertNotNull(sites);
        assertEquals(2, sites.size());
        assertModelEquals(sites.get(0), siteA);
        assertModelEquals(sites.get(1), siteC);

        sites = logic.getByFacility(facilityB.getUri(), user);

        assertNotNull(sites);
        assertEquals(2, sites.size());
        assertModelEquals(sites.get(0), siteB);
        assertModelEquals(sites.get(1), siteC);
    }

    public void testCreate() {
    }

    public void testUpdate() {
    }

    public void testDelete() {
    }

    public void testValidateSiteFacilityAddress() {
    }

    private void assertModelEquals(SiteModel expected, SiteModel actual){

        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(), actual.getUri()));
        assertEquals(expected.getName(), actual.getName());

        // assert factor levels equality
        Set<URI> expectedOrgaUris = expected.getOrganizations().stream()
                .map(orga -> SPARQLDeserializers.formatURI(orga.getUri())).collect(Collectors.toSet());

        Set<URI> actualOrgaUris = actual.getOrganizations().stream()
                .map(orga -> SPARQLDeserializers.formatURI(orga.getUri())).collect(Collectors.toSet());
        assertEquals(expectedOrgaUris, actualOrgaUris);
    }
}