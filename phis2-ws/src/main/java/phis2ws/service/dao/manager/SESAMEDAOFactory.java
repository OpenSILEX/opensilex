//**********************************************************************************************
//                                       SESAMEDAOFactory.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: List available Dao which are connected to Sesame
//***********************************************************************************************
package phis2ws.service.dao.manager;

import phis2ws.service.dao.sesame.DocumentDaoSesame;

public class SESAMEDAOFactory extends DAOFactory {
    public DocumentDaoSesame getDocumentsDaoSesame() {
        return new DocumentDaoSesame();
    }
}
