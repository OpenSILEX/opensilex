//******************************************************************************
//                                       Orders.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 07, Sep 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.configuration;

//SILEX:todo
// Use this enum instead of the Documentation in the documentation swagger examples
//\SILEX:todo

/**
 * The list of the authorized sort parameters formats.
 * If you modify orders values changes the linked documentation below.
 * @see DocumentationAnnotation.EXAMPLE_ORDER_ALLOWABLE_VALUES
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Orders {
    
    ASC {
        @Override
        public String toString(){
            return "asc";
        }
    },
    DESC {
        @Override
        public String toString(){
            return "desc";
        }
    }
}
