//******************************************************************************
//                                EventDAOTest.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 30 oct. 2019
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************


package opensilex.service.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.dao.manager.Rdf4jDAOTest;
import opensilex.service.model.Annotation;
import opensilex.service.model.Event;
import opensilex.service.model.Experiment;
import opensilex.service.model.Project;
import opensilex.service.model.ScientificObject;

/**
 * 
 * @author renaud.colin@inra.fr
 *
 */
class EventDAOTest extends Rdf4jDAOTest{
	
	protected static EventDAO eventDao;
	protected ScientificObject so;
	protected Experiment xp;
	
	@Override
	protected Rdf4jDAO<Event> getDao() {
		return eventDao;
	}

	@BeforeAll
	public static void setUp() throws Exception {
		eventDao =  new EventDAO(userDao.findById(userUri));
		initDaoWithInMemoryStoreConnection(eventDao);
	}
	
	@BeforeEach
	protected void initDao() throws DAOPersistenceException, Exception {
		Project createdProject = createAndGetProject();
		xp = createAndGetExperiment(createdProject);
		so = createAndGetScientificObject(xp);
	}

	@Test
	void test_delete_event_on_one_scientific_object() throws DAOPersistenceException, Exception {
		
		long initialSize = eventDao.getConnection().size();
		Event event = createAndGetEvent(so.getUri()); // create one event on one ScientificObject		
		eventDao.delete(Arrays.asList(event));
		
		assertEquals(initialSize,eventDao.getConnection().size());
		assertFalse(eventDao.existUri(event.getUri()));
		assertTrue(eventDao.existUri(so.getUri()));
	}
	

	@Test
	void test_delete_event_on_multiple_scientific_object() throws DAOPersistenceException, Exception {
		int k = 3;
		String[] soUris = new String[k];
		for(int i=0;i<k;i++) {
			soUris[i] = createAndGetScientificObject(xp).getUri();
		}	
		long initialSize = eventDao.getConnection().size();
		Event event = createAndGetEvent(soUris);
		eventDao.delete(Arrays.asList(event));
		assertEquals(initialSize,eventDao.getConnection().size());
		
		assertFalse(eventDao.existUri(event.getUri()));
		for(String soUri : soUris) {
			assertTrue(eventDao.existUri(soUri));
		}
		
	}
	
	@Test
	void test_delete_event_with_one_annotation() throws DAOPersistenceException, Exception {
		
		long initialSize = eventDao.getConnection().size();
		
		Event event = createAndGetEvent(so.getUri()); // create one event on one ScientificObject
		Annotation a = createAndGetAnnotation(event.getUri());
		eventDao.delete(Arrays.asList(event));
		
		assertFalse(eventDao.existUri(event.getUri()));
		assertFalse(eventDao.existUri(a.getUri()));
		assertEquals(initialSize,eventDao.getConnection().size());

	}
	
	@Test
	void test_delete_event_with_one_annotation_on_multiple_events() throws DAOPersistenceException, Exception {
		
		long initialSize = eventDao.getConnection().size();
		
		Event event = createAndGetEvent(so.getUri()); // create one event on one ScientificObject
		Event event1 = createAndGetEvent(so.getUri());
		
		Annotation a = createAndGetAnnotation(event.getUri(),event1.getUri()); // delete the first event, the annotation should not be removed
		eventDao.delete(Arrays.asList(event));
		
		assertFalse(eventDao.existUri(event.getUri()));
		assertTrue(eventDao.existUri(event1.getUri()));
		assertTrue(eventDao.existUri(a.getUri()));
		
		assertNotEquals(initialSize,eventDao.getConnection().size());
		
	}
	
	

}
