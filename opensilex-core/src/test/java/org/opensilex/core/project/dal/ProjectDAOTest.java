/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.dal;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.CoreModule;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.rest.RestModule;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author Julien BONNEFONT
 */
public class ProjectDAOTest extends AbstractUnitTest {

    private static RDF4JInMemoryServiceFactory factory;

    protected ExperimentDAO xpDao;
    protected ExperimentModel experimentModel;
    protected static String xpGraph;

    protected ProjectDAO projectDAO;
    protected static String projectGraph;

    protected static SPARQLService sparql;

    @BeforeClass
    public static void setup() throws Exception {
        OpenSilex.registerModule(RestModule.class);
        OpenSilex.registerModule(CoreModule.class);
        factory = new RDF4JInMemoryServiceFactory();
        sparql = factory.provide();

        xpGraph = SPARQLService.getDefaultGraph(ExperimentModel.class).toString();
        projectGraph = SPARQLService.getDefaultGraph(ProjectModel.class).toString();
    }

    @Before
    public void init() throws Exception {
        experimentModel = new ExperimentModel();
        experimentModel.setLabel("TEST EXPE2");
        experimentModel.setStartDate(LocalDate.now());
        xpDao = new ExperimentDAO(sparql);
        projectDAO = new ProjectDAO(sparql);
        xpDao.create(experimentModel);

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

    protected ProjectModel getModel(int i) {
        ProjectModel prjctModel = new ProjectModel();
        String label = "test pj" + i;
        prjctModel.setLabel(label);
        prjctModel.setStartDate(LocalDate.now());
        prjctModel.setEndDate(LocalDate.now().plusDays(200));
        prjctModel.setExperiments(Collections.singletonList(experimentModel));
        prjctModel.setIsPublic(true);

        return prjctModel;
    }

    @Test
    public void create() throws Exception {

        int count = sparql.count(ProjectModel.class);
        assertEquals("the initial ProjectModel count must be 0", 0, count);

        projectDAO.create(getModel(0));

        count = sparql.count(ProjectModel.class);
        assertEquals("the count must be equals to 1 since the project has been created", 1, count);
    }

    @Test
    public void createAll() throws Exception {

        int count = sparql.count(ProjectModel.class);
        assertEquals(count, 0);

        int n = 10;
        for (int i = 0; i < n; i++) {
            projectDAO.create(getModel(i));
        }

        count = sparql.count(ProjectModel.class);
        assertEquals("the count must be equals to " + n + " since " + n + " project have been created", n, count);
    }

    protected void testEquals(final ProjectModel pjModel, final ProjectModel daoPjModel) {

        final String errorMsg = "fetched object different than model";

        assertEquals(errorMsg, daoPjModel.getUri(), pjModel.getUri());
        assertEquals(errorMsg, daoPjModel.getLabel(), pjModel.getLabel());
        assertEquals(errorMsg, daoPjModel.getStartDate(), pjModel.getStartDate());
        assertEquals(errorMsg, daoPjModel.getEndDate(), pjModel.getEndDate());
        compareLists(errorMsg, daoPjModel.getGroups(), pjModel.getGroups());
    }

    private void compareLists(String errorMsg, List<?> expectedList, List<?> actualList) {
        assertTrue(errorMsg, expectedList.size() == actualList.size());
        assertTrue(errorMsg, expectedList.containsAll(actualList));
        assertTrue(errorMsg, actualList.containsAll(expectedList));
    }

    @Test
    public void getByUri() throws Exception {

        ProjectModel xpModel = getModel(0);
        projectDAO.create(xpModel);

        ProjectModel daoPjModel = projectDAO.get(xpModel.getUri());
        assertNotNull(daoPjModel);

        testEquals(xpModel, daoPjModel);
    }

    @Test
    public void update() throws Exception {

        ProjectModel pjModel = getModel(0);
        projectDAO.create(pjModel);

        // update attributes
        pjModel.setStartDate(LocalDate.now());
        pjModel.setEndDate(LocalDate.now().plusDays(300));

        pjModel.setLabel("new label");
        projectDAO.update(pjModel);

        // get the new xp model and check that all fields are equals
        ProjectModel daoPjModel = projectDAO.get(pjModel.getUri());
        assertNotNull("no Project found from db", daoPjModel);
        testEquals(pjModel, daoPjModel);
    }

    @Test(expected = SPARQLInvalidURIException.class)
    public void updateWithUnknownURI() throws Exception {

        ProjectModel pjModel = getModel(0);
        projectDAO.create(pjModel);

        pjModel.setLabel("updateWithUnknownUri");
        pjModel.setUri(new URI(pjModel.getUri().toString() + "_suffix"));
        projectDAO.update(pjModel);
    }

    @Test
    public void delete() throws Exception {

        ProjectModel pjModel = getModel(0);
        projectDAO.create(pjModel);

        int oldCount = sparql.count(ProjectModel.class);
        assertEquals("one project should have been created", 1, oldCount);

        projectDAO.delete(pjModel.getUri());

        int newCount = sparql.count(ProjectModel.class);
        assertEquals("the project must no longer exists", 0, newCount);
        assertFalse("the project URI must no longer exists", projectDAO.sparql.uriExists(pjModel.getUri()));
    }

    @Test(expected = SPARQLInvalidURIException.class)
    public void deleteWithUnknownURI() throws Exception {

        ProjectModel pjModel = getModel(0);
        projectDAO.create(pjModel);
        projectDAO.delete(new URI(pjModel.getUri().toString() + "_suffix"));
    }

    @Test
    public void deleteAll() throws Exception {

        int n = 10;
        List<ProjectModel> pjs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            pjs.add(getModel(i));
        }
        for (ProjectModel pj : pjs) {
            projectDAO.create(pj);
        }
        int oldCount = sparql.count(ProjectModel.class);
        assertEquals(n + " projects should have been created", oldCount, n);

        projectDAO.delete(pjs.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList()));

        int newCount = sparql.count(ProjectModel.class);
        assertEquals("all projects should have been deleted", 0, newCount);

        for (ProjectModel pj : pjs) {
            assertFalse("the ProjectModel " + pj.getUri() + " should have been deleted", projectDAO.sparql.uriExists(pj.getUri()));
        }
    }

}
