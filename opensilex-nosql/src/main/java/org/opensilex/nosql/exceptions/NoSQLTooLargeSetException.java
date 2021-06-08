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
public class NoSQLTooLargeSetException extends Exception{
    
    public NoSQLTooLargeSetException(int sizeMax, int size) {
        super("Too large set of data (" + String.valueOf(size) + ">" + String.valueOf(sizeMax) +")");
    }
}
