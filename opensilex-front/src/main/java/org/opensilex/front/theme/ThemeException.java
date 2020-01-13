/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.theme;

import java.io.IOException;

/**
 *
 * @author vidalmor
 */
public class ThemeException extends Exception {

    public ThemeException(String message) {
        super(message);
    }

    ThemeException(String message, Throwable t) {
         super(message, t);
    }
    
}
