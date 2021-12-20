/*******************************************************************************
 *                         PropertyModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.ontology.dal;


import java.net.URI;

/**
 *
 * @author vince
 */
public interface PropertyModel {

    ClassModel getDomain();

    void setDomain(ClassModel domain);

    URI getTypeRestriction();

    void setTypeRestriction(URI typeRestriction);

}
