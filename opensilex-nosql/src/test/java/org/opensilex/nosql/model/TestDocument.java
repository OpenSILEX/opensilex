/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.model;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

/**
 *
 * @author charlero
 */
@PersistenceCapable
public class TestDocument {
    @PrimaryKey
    String name;
    
    String description;
    
    Integer value;

    public TestDocument(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    
    
    
}
