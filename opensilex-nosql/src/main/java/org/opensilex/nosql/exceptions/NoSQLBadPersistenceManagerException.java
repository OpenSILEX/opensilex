/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.exceptions;

/**
 *
 * @author sammy
 */
public class NoSQLBadPersistenceManagerException extends Exception {

    public NoSQLBadPersistenceManagerException() {
        super("Persistent manager could not be null");
    }
    
}
