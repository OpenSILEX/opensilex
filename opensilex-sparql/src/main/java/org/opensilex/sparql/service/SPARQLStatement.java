/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;


/**
 *
 * @author vidalmor
 */
public interface SPARQLStatement {

    public String getSubject();

    public String getPredicate();

    public String getObject();

    public String getContext();
}
