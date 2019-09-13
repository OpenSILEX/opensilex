/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.config;

/**
 *
 * @author vincent
 */
public class InvalidConfigException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidConfigException(Exception ex) {
        super(ex);
    }
    
    public InvalidConfigException(String message) {
        super(message);
    }

}
