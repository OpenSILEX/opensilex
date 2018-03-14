//**********************************************************************************************
//                                       DocumentDao.java 
//
// Author(s): Arnaud CHARLEROY, Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2017
// Subject: A Dao specific to document insertion into PHIS DB
//***********************************************************************************************
package phis2ws.service.dao.phis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOPhisBrapi;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sql.SQLQueryBuilder;
import phis2ws.service.view.model.phis.Document;

public class DocumentDao extends DAOPhisBrapi<Document, Object> {
    final static Logger LOGGER = LoggerFactory.getLogger(DocumentDao.class);

    public DocumentDao() {
        super();
        setTable("document");
    }

    @Override
    public Integer count() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> pkeySQLFieldLink() {
        Map<String, String> pkeySQLFieldLink = new HashMap<>();
        pkeySQLFieldLink.put("id", "id");
        pkeySQLFieldLink.put("name", "name");
        pkeySQLFieldLink.put("uri", "uri");
        return pkeySQLFieldLink;
    }

    @Override
    public Map<String, String> relationFieldsJavaSQLObject() {
        Map<String, String> createSQLFields = new HashMap<>();
        createSQLFields.put("name", "name");
        createSQLFields.put("type", "type");
        createSQLFields.put("date", "date");
        createSQLFields.put("path", "path");
        createSQLFields.put("targetTable", "target_table");
        createSQLFields.put("targetPk", "target_pk");
        createSQLFields.put("version", "version");
        createSQLFields.put("uri", "uri");
        
        return createSQLFields;
    }

    @Override
    public Document findByFields(Map<String, Object> Attr, String table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document single(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Document> all() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document get(ResultSet result) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Document> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndInsert(Object newObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndInsertList(List<Object> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Document compareAndMergeObjects(Document fromDB, Document object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public POSTResultsReturn checkAndUpdateList(List<Object> newObjects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
