//******************************************************************************
//                                StudyRDFDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 2 mai 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Study RDF DAO - used to filter studies on the germplasms
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyRDFDAO extends Rdf4jDAO {
    public ArrayList<String> germplasmDbIds;
    
    /**
     * Builds the SparQL query to get studies filtered on germplasms
     * @example 
     * SELECT DISTINCT ?expURI 
     * WHERE { ?scientificObjectURI <http://www.opensilex.org/vocabulary/oeso#hasGermplasm> ?g19001 .
     *          ?scientificObjectURI oeso:participatesIn ?expURI}
     *
     * @return the SparQL query
     */
    private  SPARQLQueryBuilder prepareSearchOnGermplasmsQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendSelect("?expURI");

        for (String gp:germplasmDbIds) {
            query.appendTriplet("?scientificObjectURI","<http://www.opensilex.org/vocabulary/oeso#hasGeneticResource>", gp, null);
            query.appendTriplet("?scientificObjectURI","<http://www.opensilex.org/vocabulary/oeso#participatesIn>", "?expURI", null);
        
        }
        return query;         
    }
    
    /**
     * Get the studies URIs linked to chosen germplasms
     *
     * @return the list of study URIs
     */
    public ArrayList<String> getStudiesFromGermplasms() throws DAOPersistenceException {
        SPARQLQueryBuilder query = prepareSearchOnGermplasmsQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        ArrayList<String> studiesURI = new ArrayList();
        try {
            TupleQueryResult studiesURIResult = tupleQuery.evaluate();
            while (studiesURIResult.hasNext()) {
                String studyURI = getStringValueOfSelectNameFromBindingSet("expURI", studiesURIResult.next());
                if (studyURI != null) {
                    studiesURI.add(studyURI);
                }
            }
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex); 
        }
        return studiesURI;        
    }

    @Override
    public List create(List objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List update(List objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object find(Object object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
