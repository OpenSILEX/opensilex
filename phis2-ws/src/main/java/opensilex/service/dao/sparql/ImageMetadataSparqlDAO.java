//**********************************************************************************************
//                                       ImageMetadataDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: A Dao sepecific to images metadata
//***********************************************************************************************
package opensilex.service.dao.sparql;

import java.util.List;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.SparqlDAO;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.ImageMetadata;

/**
 * Used to check if an image type exist in the triplestore
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageMetadataSparqlDAO extends SparqlDAO<ImageMetadata> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ImageMetadataSparqlDAO.class);
    
    public String rdfType;

    public ImageMetadataSparqlDAO() {
    }

    @Override
    public List create(List objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List update(List objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ImageMetadata find(ImageMetadata object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ImageMetadata findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
