//******************************************************************************
//                          ExperimentDAOTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************


package org.opensilex.core.experiment.dal;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.*;

import org.opensilex.OpenSilex;
import org.opensilex.core.CoreModule;
import org.opensilex.rest.RestModule;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryService;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 * @author Renaud COLIN
 * @author Vincent MIGOT
 */
public class ExperimentDAOTest extends AbstractUnitTest{

    private static RDF4JInMemoryService factory;

    protected ExperimentDAO xpDao;
    protected static String xpGraph;

    protected ProjectDAO projectDAO;
    protected ProjectModel projectModel;
    protected static String projectGraph;

    protected static SPARQLService sparql;

    @BeforeClass
    public static void setup() throws Exception {
        OpenSilex.registerModule(RestModule.class);
        OpenSilex.registerModule(CoreModule.class);
        factory = new RDF4JInMemoryService();
        sparql = factory.provide();

        xpGraph = SPARQLClassObjectMapper.getForClass(ExperimentModel.class).getDefaultGraph().toString();
        projectGraph = SPARQLClassObjectMapper.getForClass(ProjectModel.class).getDefaultGraph().toString();
    }

    @Before
    public void init() throws Exception {
        projectModel = new ProjectModel();
        projectModel.setName("TEST PROJECT");
        projectDAO = new ProjectDAO(sparql, null);
        xpDao = new ExperimentDAO(sparql);
        projectDAO.create(projectModel);
    }

    @After
    public void clean() throws Exception {
        SPARQLModule.clearPlatformGraphs(sparql, Arrays.asList(xpGraph, projectGraph));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        sparql.clear();
        factory.dispose(sparql);
    }

    protected ExperimentModel getModel(int i) {

        ExperimentModel xpModel = new ExperimentModel();
        String label = "test xp" + i;
        xpModel.setLabel(label);
        xpModel.setStartDate(LocalDate.now());
        xpModel.setEndDate(LocalDate.now().plusDays(200));
        xpModel.setCampaign(2019);
        xpModel.setIsPublic(true);

        xpModel.setProjects(Collections.singletonList(projectModel));
        xpModel.setComment("a comment about an xp");
        xpModel.setKeywords(Arrays.asList("test", "project", "opensilex"));
        xpModel.setObjective("this project has for objective to pass all the TU !");

        return xpModel;
    }

    @Test
    public void create() throws Exception {

        int count = sparql.count(ExperimentModel.class, null);
        assertEquals("the initial ExperimentModel count must be 0", 0, count);

        xpDao.create(getModel(0));

        count = sparql.count(ExperimentModel.class, null);
        assertEquals("the count must be equals to 1 since the experiment has been created", 1, count);
    }

    @Test
    public void createAll() throws Exception {

        int count = sparql.count(ExperimentModel.class, null);
        assertEquals(count, 0);

        int n = 10;
        for (int i = 0; i < n; i++) {
            xpDao.create(getModel(i));
        }

        count = sparql.count(ExperimentModel.class, null);
        assertEquals("the count must be equals to " + n + " since " + n + " experiment have been created", n, count);
    }

    protected void testEquals(final ExperimentModel xp, final ExperimentModel daoXpModel) {

        final String errorMsg = "fetched object different than model";

        assertEquals(errorMsg, daoXpModel.getUri(), xp.getUri());
        assertEquals(errorMsg, daoXpModel.getLabel(), xp.getLabel());
        assertEquals(errorMsg, daoXpModel.getStartDate(), xp.getStartDate());
        assertEquals(errorMsg, daoXpModel.getEndDate(), xp.getEndDate());
        assertEquals(errorMsg, daoXpModel.getCampaign(), xp.getCampaign());
        assertEquals(errorMsg, daoXpModel.getObjective(), xp.getObjective());
        assertEquals(errorMsg, daoXpModel.getComment(), xp.getComment());
        compareLists(errorMsg, daoXpModel.getKeywords(), xp.getKeywords());
        compareLists(errorMsg, daoXpModel.getProjects(), xp.getProjects());
        compareLists(errorMsg, daoXpModel.getScientificSupervisors(), xp.getScientificSupervisors());
        compareLists(errorMsg, daoXpModel.getTechnicalSupervisors(), xp.getTechnicalSupervisors());
        compareLists(errorMsg, daoXpModel.getGroups(), xp.getGroups());
        compareLists(errorMsg, daoXpModel.getVariables(), xp.getVariables());
        compareLists(errorMsg, daoXpModel.getSensors(), xp.getSensors());
    }

    private void compareLists(String errorMsg, List<?> expectedList, List<?> actualList) {
        assertTrue(errorMsg, expectedList.size() == actualList.size());
        assertTrue(errorMsg, expectedList.containsAll(actualList));
        assertTrue(errorMsg, actualList.containsAll(expectedList));
    }

    @Test
    public void getByUri() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentModel daoXpModel = xpDao.get(xpModel.getUri());
        assertNotNull(daoXpModel);

        testEquals(xpModel, daoXpModel);
    }

    @Test
    public void getAllXp() throws Exception {

        int n = 10;
        for (int i = 0; i < n; i++) {
            xpDao.create(getModel(i));
        }

        int pageSize = 10;
        int nbPage = n / pageSize;
        for (int i = 0; i < nbPage; i++) {
            ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(null, null, i, pageSize);
            List<ExperimentModel> xpsFromDao = xpModelResults.getList();
            assertEquals(pageSize, xpsFromDao.size());
        }

    }

    @Test
    public void searchWithDataType() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentSearchDTO searchDTO = new ExperimentSearchDTO();
        searchDTO.setLabel(xpModel.getLabel())
                .setCampaign(xpModel.getCampaign())
                .setStartDate(xpModel.getStartDate().toString())
                .setIsPublic(true);

        ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(searchDTO, null, 0, 10);
        assertNotNull("one experiment should be fetched from db", xpModelResults);
        assertEquals("one experiment should be fetched from db", 1, xpModelResults.getList().size());

        testEquals(xpModel, xpModelResults.getList().get(0));
    }

//    @Test
//    public void searchWithDataTypeList() throws Exception {
//
//        ExperimentModel model = getModel(0);
//        ExperimentModel model2 = getModel(1);
//
//        URI varUri = new URI(Oeso.Variable.getURI() + "/var1");
//        URI var2Uri = new URI(Oeso.Variable.getURI() + "/var2");
//        model.getVariables().addAll(Arrays.asList(varUri, var2Uri));
//        model2.getVariables().add(varUri);
//
//        xpDao.create(model);
//        xpDao.create(model2);
//
//        ExperimentSearchDTO searchDTO = new ExperimentSearchDTO();
//        searchDTO.getVariables().add(var2Uri);
//
//        // search all xp with the two variable URI
//        ListWithPagination<ExperimentModel> searchXps = xpDao.search(searchDTO, Collections.emptyList(), 0, 20);
//        List<ExperimentModel> xpList = searchXps.getList();
//
//        assertEquals(1, xpList.size());
//        assertTrue(xpList.contains(model));
//
//        // search all xp with only one variable URI
//        searchDTO = new ExperimentSearchDTO();
//        searchDTO.getVariables().add(varUri);
//        searchXps = xpDao.search(searchDTO, Collections.emptyList(), 0, 20);
//        xpList = searchXps.getList();
//
//        assertEquals(2, xpList.size());
//        assertTrue(xpList.contains(model));
//        assertTrue(xpList.contains(model2));
//    }

    @Test
    public void searchWithObjectUriType() throws Exception {

        // create two projects
        List<ProjectModel> projects = new ArrayList<>();
        projects.add(projectModel);

        ProjectModel project2 = new ProjectModel();
        project2.setName("TEST PROJECT");
        projects.add(projectDAO.create(project2));

        ExperimentModel xpModel = getModel(0);
        xpModel.setProjects(projects);
        xpDao.create(xpModel);

        ExperimentSearchDTO searchDTO = new ExperimentSearchDTO();
        searchDTO.setProjects(Arrays.asList(projectModel.getUri(), project2.getUri()));

        List<ExperimentModel> xpModelResults = xpDao.search(searchDTO, null, 0, 10).getList();

        assertNotNull("no experiment found from db", xpModelResults);
        assertEquals("the experiment uri should be contained into the result list", xpModelResults.get(0).getUri(), xpModel.getUri());
        testEquals(xpModel, xpModelResults.get(0));

        // create a 2nd xp with a shared project with the 1th xp
        ExperimentModel xpModel2 = getModel(1);
        xpModel2.setProjects(Collections.singletonList(project2));
        xpDao.create(xpModel2);

        xpModelResults = xpDao.search(searchDTO, null, 0, 10).getList();
        assertNotNull("no experiment found from db", xpModelResults);

        assertTrue(xpModelResults.contains(xpModel2));
        assertTrue(xpModelResults.contains(xpModel));

    }

    @Test
    public void searchFail() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentSearchDTO getDTO = new ExperimentSearchDTO();

        // set a bad label in order to check if the result set from dao is empty
        getDTO.setLabel(xpModel.getLabel() + "str");

        getDTO.setProjects(xpModel.getProjects()
                .stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));

        ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(getDTO, null, 0, 10);
        assertNotNull("empty results object", xpModelResults);
        assertTrue("no experiment should be found from db according getDTO", xpModelResults.getList().isEmpty());

        // reset the label by setting it to null, then a result must exists
        getDTO.setLabel(null);
        getDTO.setCampaign(xpModel.getCampaign());

        xpModelResults = xpDao.search(getDTO, null, 0, 10);
        assertNotNull("no experiment found from db", xpModelResults);
        assertEquals("the experiment uri should be contained into the result list", xpModelResults.getList().get(0).getUri(), xpModel.getUri());
        testEquals(xpModel, xpModelResults.getList().get(0));

    }

    @Test
    public void searchEnded() throws Exception {

        // create an archived and an unarchived xp
        LocalDate currentDate = LocalDate.now();
        ExperimentModel archivedXp = getModel(0);
        archivedXp.setStartDate(currentDate.minusDays(3));
        archivedXp.setEndDate(currentDate.minusDays(1));
        xpDao.create(archivedXp);

        ExperimentModel unarchivedXp = getModel(1);
        unarchivedXp.setStartDate(currentDate.minusDays(3));
        unarchivedXp.setEndDate(currentDate.plusDays(3));
        xpDao.create(unarchivedXp);

        // try to retrieve xps from dao
        List<ExperimentModel> archivedXps = xpDao.search(new ExperimentSearchDTO().setEnded(true), null, 0, 10).getList();
        assertEquals(1, archivedXps.size());
        assertTrue(archivedXps.contains(archivedXp));

        List<ExperimentModel> unarchivedXps = xpDao.search(new ExperimentSearchDTO().setEnded(false), null, 0, 10).getList();
        assertEquals(1, unarchivedXps.size());
        assertTrue(unarchivedXps.contains(unarchivedXp));

        // search all archived projects
        List<ExperimentModel> allXps = xpDao.search(new ExperimentSearchDTO(), null, 0, 10).getList();
        assertEquals(2, allXps.size());
        assertTrue(allXps.contains(archivedXp));
        assertTrue(allXps.contains(unarchivedXp));

    }

    @Test
    public void update() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        // update attributes
        xpModel.setStartDate(LocalDate.now());
        xpModel.setEndDate(LocalDate.now().plusDays(300));

        xpModel.setLabel("new label");
        xpModel.setComment("new comments about model");
        xpModel.setObjective("new objective");
        xpModel.setCampaign(2020);
        xpDao.update(xpModel);

        // get the new xp model and check that all fields are equals
        ExperimentModel daoXpModel = xpDao.get(xpModel.getUri());
        assertNotNull("no experiment found from db", daoXpModel);
        testEquals(xpModel, daoXpModel);
    }

    @Test(expected = SPARQLInvalidURIException.class)
    public void updateWithUnknownURI() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        xpModel.setLabel("updateWithUnknownUri");
        xpModel.setUri(new URI(xpModel.getUri().toString() + "_suffix"));
        xpDao.update(xpModel);
    }

    @Test
    public void delete() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        int oldCount = sparql.count(ExperimentModel.class, null);
        assertEquals("one experiment should have been created", 1, oldCount);

        xpDao.delete(xpModel.getUri());

        int newCount = sparql.count(ExperimentModel.class, null);
        assertEquals("the experiment must no longer exists", 0, newCount);
        assertFalse("the experiment URI must no longer exists", xpDao.sparql.uriExists(xpModel.getUri()));
    }

    @Test(expected = SPARQLInvalidURIException.class)
    public void deleteWithUnknownURI() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);
        xpDao.delete(new URI(xpModel.getUri().toString() + "_suffix"));
    }

    @Test
    public void deleteAll() throws Exception {

        int n = 10;
        List<ExperimentModel> xps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            xps.add(getModel(i));
        }
        for (ExperimentModel xp : xps) {
            xpDao.create(xp);
        }
        int oldCount = sparql.count(ExperimentModel.class, null);
        assertEquals(n + " experiments should have been created", oldCount, n);

        xpDao.delete(xps.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList()));

        int newCount = sparql.count(ExperimentModel.class, null);
        assertEquals("all experiments should have been deleted", 0, newCount);

        for (ExperimentModel xp : xps) {
            assertFalse("the ExperimentModel " + xp.getUri() + " should have been deleted", xpDao.sparql.uriExists(xp.getUri()));
        }
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void testCreateWithUnknownSpecies() throws Exception {
//        ExperimentModel xp = getModel(0);
//        xp.setSpecies(new URI("http://www.opensilex.org/id/species/zeamays"));
//        xpDao.create(xp);
//    }
}
