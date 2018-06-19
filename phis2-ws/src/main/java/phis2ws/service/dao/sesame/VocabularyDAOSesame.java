//******************************************************************************
//                                       VocabularyDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 18 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  18 juin 2018
// Subject: dao to get the vocabulary (and add data) of the phis instance
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.resources.dto.PropertyVocabularyDTO;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;

/**
 * dao to get the vocabulary (and add data) of the phis instance
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VocabularyDAOSesame extends DAOSesame<Object> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(VocabularyDAOSesame.class);
    
    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    
    final static String TRIPLESTORE_RELATION_COMMENT = NAMESPACES.getRelationsProperty("comment");
    final static String TRIPLESTORE_RELATION_LABEL = NAMESPACES.getRelationsProperty("label");
    final static String LANGUAGE_EN = "en";
    final static String LABEL_LABEL_EN = "label";
    final static String LABEL_COMMENT_EN = "comment";

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public ArrayList<PropertyVocabularyDTO> allPaginateRdfsProperties() {
        ArrayList<PropertyVocabularyDTO> rdfsPropertyes = new ArrayList<>();
        
        //label property
        PropertyVocabularyDTO label = new PropertyVocabularyDTO();
        label.setRelation(TRIPLESTORE_RELATION_LABEL);
        label.addLabel(LANGUAGE_EN, LABEL_LABEL_EN);
        rdfsPropertyes.add(label);
        
        //comment property
        PropertyVocabularyDTO comment = new PropertyVocabularyDTO();
        comment.setRelation(TRIPLESTORE_RELATION_COMMENT);
        comment.addLabel(LANGUAGE_EN, LABEL_COMMENT_EN);
        rdfsPropertyes.add(comment);
        
        return rdfsPropertyes;
    }
}
