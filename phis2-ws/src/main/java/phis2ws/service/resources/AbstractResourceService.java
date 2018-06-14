//******************************************************************************
//                                       AbstractResourceService.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 juin 2018
// Contact: marnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 juin 2018
// Subject:
//******************************************************************************
package phis2ws.service.resources;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.injection.SessionInject;

/**
 * Merge common Resource service. for exemple logger
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class AbstractResourceService {
    //For java 7+, MethodHandles.lookup() return the class which call this function
    final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    //user session
    @SessionInject
    Session userSession;
}
