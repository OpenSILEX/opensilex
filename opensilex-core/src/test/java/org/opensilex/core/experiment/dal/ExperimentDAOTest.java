package org.opensilex.core.experiment.dal;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractResourceModelTest;
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.ListWithPagination;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Renaud COLIN
 */
public class ExperimentDAOTest extends AbstractResourceModelTest {

    protected static ExperimentDAO xpDao;
    protected static final String xpGraph = "set/experiments";

    protected static ProjectDAO projectDAO;
    protected static ProjectModel projectModel;
    protected static final String projectGraph = "project";

    @BeforeClass
    public static void initialize() throws Exception {

        AbstractResourceModelTest.initialize();

        xpDao = new ExperimentDAO(service);
        projectDAO = new ProjectDAO(service);
    }

    @Before
    public void initGraph() throws Exception {
        projectModel = new ProjectModel();
        projectModel.setName("TEST PROJECT");
        projectDAO.create(projectModel);
    }

    @Override
    protected List<String> getGraphsToCleanNames() {
        return Arrays.asList(xpGraph, projectGraph);
    }

    protected ExperimentModel getModel(int i) {

        ExperimentModel xpModel = new ExperimentModel();
        String label = "test xp" + i;
        xpModel.setLabel(label);
        xpModel.setStartDate(LocalDate.now());
        xpModel.setEndDate(LocalDate.now().plusDays(200));
        xpModel.setCampaign(2019);

        xpModel.setProjects(Collections.singletonList(projectModel));
        // #TODO add scientific and technical supervisors
        xpModel.setComment("a comment about an xp");
        xpModel.setKeywords(Arrays.asList("test", "project", "opensilex"));
        xpModel.setObjectives("this project has for objective to pass all the TU !");

        return xpModel;
    }

    @Test
    public void testCreate() throws Exception {

        int count = service.count(ExperimentModel.class, null);
        assertEquals("the initial ExperimentModel count must be 0", count, 0);

        xpDao.create(getModel(0));

        count = service.count(ExperimentModel.class, null);
        assertEquals("the count must be equals to 1 since the experiment has been created", count, 1);
    }

    @Test
    public void testCreateAll() throws Exception {

        int count = service.count(ExperimentModel.class, null);
        assertEquals(count, 0);

        int n = 20;
        int batchSize = 5;
        int batchIdx = 0;

        // initial xp list filling
        List<ExperimentModel> xps = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            xps.add(getModel(i));
        }
        xpDao.createAll(xps);

        for (int i = batchSize; i < n; i++) {
            xps.set(batchIdx++, getModel(i));
            if (batchIdx == batchSize) {
                xps = xpDao.createAll(xps);
                batchIdx = 0;
            }
        }
        // ensure that all xp have been created
        if (batchIdx > 0) {
            xpDao.createAll(xps.subList(0, batchIdx));
        }

        count = service.count(ExperimentModel.class, null);
        assertEquals("the count must be equals to " + n + " since " + n + " experiment have been created", count, n);
    }

    protected void testEquals(final ExperimentModel xp, final ExperimentModel otherXp) {

        final String errorMsg = "fetched object different than model";

        assertEquals(errorMsg, otherXp.getUri(), xp.getUri());
        assertEquals(errorMsg, otherXp.getLabel(), xp.getLabel());
        assertEquals(errorMsg, otherXp.getStartDate(), xp.getStartDate());
        assertEquals(errorMsg, otherXp.getEndDate(), xp.getEndDate());
        assertEquals(errorMsg, otherXp.getCampaign(), xp.getCampaign());
        assertEquals(errorMsg, otherXp.getObjectives(), xp.getObjectives());
        assertEquals(errorMsg, otherXp.getComment(), xp.getComment());
        assertEquals(errorMsg, otherXp.getKeywords(), xp.getKeywords());
        assertEquals(errorMsg, otherXp.getProjects(), xp.getProjects());
        assertEquals(errorMsg, otherXp.getScientificSupervisors(), xp.getScientificSupervisors());
        assertEquals(errorMsg, otherXp.getTechnicalSupervisors(), xp.getTechnicalSupervisors());
        assertEquals(errorMsg, otherXp.getGroups(), xp.getGroups());
    }

    @Test
    public void testGetByUri() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentModel daoXpModel = xpDao.get(xpModel.getUri());
        assertNotNull(daoXpModel);

        testEquals(xpModel, daoXpModel);

    }

    @Test
    public void testSearchSuccess() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentGetDTO getDTO = new ExperimentGetDTO();
        getDTO.setLabel(xpModel.getLabel());
        getDTO.setCampaign(xpModel.getCampaign());
        getDTO.setStartDate(xpModel.getStartDate());

        ListWithPagination<ExperimentModel> xpModelResults = xpDao.search(getDTO, null, 0, 10);
        assertNotNull("one experiment should be fetched from db", xpModelResults);
        assertEquals("one experiment should be fetched from db", xpModelResults.getList().size(), 1);

        testEquals( xpModel, xpModelResults.getList().get(0));
    }

    @Test
    public void testSearchFail() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        ExperimentGetDTO getDTO = new ExperimentGetDTO();

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
    public void testUpdate() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        // update attributes
        xpModel.setStartDate(LocalDate.now());
        xpModel.setEndDate(LocalDate.now().plusDays(300));

        xpModel.setLabel("new label");
        xpModel.setComment("new comments about model");
        xpModel.setObjectives("new objective");
        xpModel.setCampaign(2020);
        xpDao.update(xpModel);

        // get the new xp model and check that all fields are equals
        ExperimentModel daoXpModel = xpDao.get(xpModel.getUri());
        assertNotNull("no experiment found from db", daoXpModel);
        testEquals(xpModel, daoXpModel);
    }

    @Test
    public void testDelete() throws Exception {

        ExperimentModel xpModel = getModel(0);
        xpDao.create(xpModel);

        int oldCount = service.count(ExperimentModel.class, null);
        assertEquals("one experiment should have been created", oldCount, 1);

        xpDao.delete(xpModel.getUri());

        int newCount = service.count(ExperimentModel.class, null);
        assertEquals("the experiment must no longer exists", newCount, 0);
        assertFalse("the experiment URI must no longer exists", xpDao.sparql.uriExists(xpModel.getUri()));
    }

    @Test
    public void testDeleteAll() throws Exception {

        int n = 20;
        List<ExperimentModel> xps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            xps.add(getModel(i));
        }
        xpDao.createAll(xps);

        int oldCount = service.count(ExperimentModel.class, null);
        assertEquals(n + " experiments should have been created", oldCount, n);

        xpDao.deleteAll(xps.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList()));

        int newCount = service.count(ExperimentModel.class, null);
        assertEquals("all experiments should have been deleted", newCount, 0);

        for (ExperimentModel xp : xps) {
            assertFalse("the ExperimentModel " + xp.getUri() + " should have been deleted", xpDao.sparql.uriExists(xp.getUri()));
        }
    }

    @Test
    public void testUpdateWithVariableList() {

    }

    @Test
    public void testUpdateWithSensorsList() {

    }
}