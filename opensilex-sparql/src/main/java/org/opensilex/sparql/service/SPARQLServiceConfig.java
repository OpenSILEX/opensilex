/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import org.opensilex.service.ServiceConfig;

/**
 *
 * @author vince
 */
public interface SPARQLServiceConfig extends ServiceConfig {

    SPARQLConnection connection();

}
