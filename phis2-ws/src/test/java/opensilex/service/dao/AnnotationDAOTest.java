//******************************************************************************
//                                AnnotationDAOTest.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 22 oct. 2019
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************


package opensilex.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;
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
import opensilex.service.ontology.Oa;

public class AnnotationDAOTest extends Rdf4jDAOTest {
	
	/**
	 * The {@link Rdf4jDAO} to test
	 */
	private static AnnotationDAO annotationDao;
	private static List<Event> events;
	private static ScientificObject so;

	@BeforeAll
	public static void setup() throws DAOPersistenceException, Exception {
		annotationDao =  new AnnotationDAO(userDao.findById(userUri));
		initDaoWithInMemoryStoreConnection(annotationDao);
		events = new LinkedList<>();
	}
		
	@Override
	protected Rdf4jDAO<?> getDao() {
		return annotationDao;
	}
	
	
	@BeforeEach
	protected void initDao() throws DAOPersistenceException, Exception {
		
		Project createdProject = createAndGetProject();
		Experiment xp = createAndGetExperiment(createdProject);
		so = createAndGetScientificObject(xp);
		events.clear();
		events.add(createAndGetEvent(so.getUri()));
	}
	
	
	@Test
	/**
	 * Try to delete an annotation about one event 
	 */
	void test_delete_annotation() throws DAOPersistenceException, Exception {
		
		long initialSize = annotationDao.getConnection().size();	
		Annotation a = createAndGetAnnotation(events.get(0).getUri());
		annotationDao.delete(Arrays.asList(a));
		assertEquals(annotationDao.getConnection().size(), initialSize);  
		assertFalse(annotationDao.existUri(a.getUri()));
	}
	
	@Test
	/**
	 * Try to delete an annotation about one event. This annotation also have severals annotation.  
	 */
	void test_delete_annotation_with_one_super_annotation() throws DAOPersistenceException, Exception {
		
		long initialSize = annotationDao.getConnection().size();

		Annotation a  = createAndGetAnnotation(events.get(0).getUri());		
		Annotation a1 = createAndGetAnnotation(a.getUri()); // create annotation on the last created annotation
		
		annotationDao.delete(Arrays.asList(a));
		assertEquals(annotationDao.getConnection().size(), initialSize);  
		assertFalse(annotationDao.existUri(a.getUri()));
		assertFalse(annotationDao.existUri(a1.getUri()));

	}
	
	@Test
	/**
	 * Try to delete an annotation about one event. This annotation is annotated 
	 * by a annotation a1 which also have an annotation.
	 * A simple RDF representation would be 
	 * <annotation1,oa:target,event_1>, <annotation2,oa:target,annotation1>, ... , <annotation_k,oa:target,annotation_k-1>
	 * 
	 */
	void test_delete_annotation_with_recursive_annotation_chain() throws DAOPersistenceException, Exception {
		
		long initialSize = annotationDao.getConnection().size();

		int k = 2;
		String userUri = "http://www.opensilex.org/demo/id/agent/admin_phis";
		
		Annotation a = new Annotation(null,DateTime.now(),userUri,Arrays.asList("annotate an event"),
				Oa.INSTANCE_DESCRIBING.toString(),Arrays.asList(events.get(0).getUri()));		
		
		List<Annotation> annotationList = new ArrayList<>();
		annotationList.addAll(annotationDao.create(Arrays.asList(a))); // create the first annotation A and add it
		
		int nbTripleCreated = 6;
		for(int i=1;i<k+1;i++) {
			Annotation ai = new Annotation(null,DateTime.now(),userUri,Arrays.asList("annotate an annotation "+i),
					Oa.INSTANCE_DESCRIBING.toString(),Arrays.asList(annotationList.get(i-1).getUri()));			
			annotationList.addAll(annotationDao.create(Arrays.asList(ai))); // add the created annotation to the list
			nbTripleCreated += 6;
			assertEquals(annotationDao.getConnection().size(), initialSize+nbTripleCreated); // we should have delete 6 triples for each k annotation	
		}
		annotationDao.delete(Arrays.asList(annotationList.get(0)));
		assertEquals(annotationDao.getConnection().size(), initialSize); // we should have delete 6 triples for each k annotation
		
	}
	
	@Test
	/**
	 * @throws Exception 
	 * @throws DAOPersistenceException 
	 * 
	 */
	void test_delete_annotation_with_one_super_annotation_with_another_target() throws DAOPersistenceException, Exception {
		
		Annotation a = createAndGetAnnotation(events.get(0).getUri());
		events.add(createAndGetEvent(so.getUri())); // create a new event 
		
		Annotation a1 = createAndGetAnnotation(a.getUri(),events.get(1).getUri());
		
		annotationDao.delete(Arrays.asList(a));
		assertFalse(annotationDao.existUri(a.getUri()));
		assertTrue(annotationDao.existUri(a1.getUri()));

	}



}
