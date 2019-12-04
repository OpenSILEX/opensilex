//******************************************************************************
//                            Rdf4jDAOTest.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 10 October 2019
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.manager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;

import opensilex.service.dao.AnnotationDAO;
import opensilex.service.dao.EventDAO;
import opensilex.service.dao.ExperimentRdf4jDAO;
import opensilex.service.dao.ProjectDAO;
import opensilex.service.dao.ScientificObjectRdf4jDAO;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.model.Annotation;
import opensilex.service.model.Event;
import opensilex.service.model.Experiment;
import opensilex.service.model.Project;
import opensilex.service.model.ScientificObject;
import opensilex.service.ontology.Oa;
import opensilex.service.ontology.Oeev;
import opensilex.service.ontology.Oeso;
import opensilex.service.utils.UriGenerator;

/**
 * 
 * This class is an utility class for building unit test on a {@link Rdf4jDAO} class instance. <br>
 * @author renaud.colin@inra.fr
 */

public abstract class Rdf4jDAOTest {
	
	
	private static Repository memoryRepository;
	
	
	/**
	 * Setup the test by instanciating a {@link Rdf4jDAO} and initializing the dao {@link RepositoryConnection} <br>
	 * Use {@link BeforeAll} annotation in order to intialize the connection before each test run
	 */
				
	/**
	 * Set a {@link SailRepository} with a {@link MemoryStore} as repository for the dao. <br>
	 * @param dao
	 */
	protected static void initDaoWithInMemoryStoreConnection(Rdf4jDAO<?> dao) {
		
		Objects.requireNonNull(dao);	
		if(memoryRepository == null) {
			memoryRepository = new SailRepository(new MemoryStore());
			memoryRepository.initialize();
		}
		RepositoryConnection memoryConn = memoryRepository.getConnection(); 
		dao.getConnection().close(); // close the old connection
		dao.setConnection(memoryConn);
		
		System.out.println("Connection to repository [OK]");
	}
	
	@AfterEach
	public void cleanStore() {
		assert(memoryRepository != null);		
		RepositoryConnection conn = memoryRepository.getConnection(); 
		if(conn != null && conn.isOpen()) {
			long size = conn.size();
			System.out.println("DAO size before clear() : "+size);
			conn.clear();

		}
	}
	

	/**
	 * 
	 * @return
	 * @throws DAOPersistenceException
	 * @throws Exception
	 */
	protected Project createAndGetProject() throws DAOPersistenceException, Exception {
			
		Project project = new Project();
		project.setName("project");
		project.setShortname(project.getName());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		project.setStartDate(LocalDateTime.now().format(formatter));
		project.setEndDate(LocalDateTime.now().format(formatter));
		
		ProjectDAO projectDAO = new ProjectDAO();
		initDaoWithInMemoryStoreConnection(projectDAO);		
		return projectDAO.create(Arrays.asList(project)).get(0);
	}
	
	/**
	 * 
	 * @param project
	 * @return
	 * @throws DAOPersistenceException
	 * @throws Exception
	 */
	protected Experiment createAndGetExperiment(Project project) throws DAOPersistenceException, Exception {
				
		Experiment xp = new Experiment();
		xp.addProject(project);
		xp.setAlias("experiment");
		xp.setStartDate(project.getStartDate());
		xp.setEndDate(project.getEndDate());
		xp.setCampaign("2019");
		xp.setUri(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_EXPERIMENT.toString(), xp.getCampaign(), null));
		
		ExperimentRdf4jDAO xpDao = new ExperimentRdf4jDAO();
		initDaoWithInMemoryStoreConnection(xpDao);		
		xpDao.insertExperiments(Arrays.asList(xp)); // #FIXME	use xpDao.create(List) 
		return xp;
	}
	
	/**
	 * 
	 * @param xp
	 * @return
	 * @throws Exception
	 */
	protected ScientificObject createAndGetScientificObject(Experiment xp) throws Exception {	
		ScientificObject so = new ScientificObject();
		so.setLabel("so");
		so.setExperiment(xp.getUri());
		so.setRdfType(Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString());

		ScientificObjectRdf4jDAO soDao = new ScientificObjectRdf4jDAO();
		initDaoWithInMemoryStoreConnection(soDao);
		return (ScientificObject) soDao.create(Arrays.asList(so)).get(0);
	}
	
	/**
	 * 
	 * @param nbEvent
	 * @param targetUri
	 * @return
	 * @throws DAOPersistenceException
	 * @throws Exception
	 */
	protected Annotation createAndGetAnnotation(String creatorUri, String... targetUris) throws DAOPersistenceException, Exception {
		
		AnnotationDAO annotationDao = new AnnotationDAO();
		initDaoWithInMemoryStoreConnection(annotationDao);
		
		ArrayList<String> bodyValues = new ArrayList<String>();
		bodyValues.add("annotate an event");
		
		ArrayList<String> targetsList = new ArrayList<String>(targetUris.length);
		for(String target : targetUris)
			targetsList.add(target);
		
		Annotation a = new Annotation(null,DateTime.now(),creatorUri,bodyValues,Oa.INSTANCE_DESCRIBING.toString(),targetsList);	
		return annotationDao.create(Arrays.asList(a)).get(0);	
	}
	
	/**
	 * 
	 * @param e
	 * @return
	 * @throws Exception 
	 * @throws DAOPersistenceException 
	 */
	protected Event createAndGetEvent(String... concernedItemUris) throws DAOPersistenceException, Exception {
		
		EventDAO eventDao = new EventDAO(null);
		initDaoWithInMemoryStoreConnection(eventDao);
		
		ArrayList<String> concernedUrisList = new ArrayList<String>(concernedItemUris.length);	
		Event event  = new Event(null, Oeev.Event.getURI(),concernedUrisList, new DateTime(), new ArrayList<>(1),new ArrayList<>(1));
		eventDao.create(Arrays.asList(event));
		return event;	
	}

}
