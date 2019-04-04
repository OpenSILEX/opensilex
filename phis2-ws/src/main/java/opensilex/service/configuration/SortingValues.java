//******************************************************************************
//                              SortingValues.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 Sep. 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.configuration;

//SILEX:todo
// Use this enum instead of the Documentation in the documentation swagger examples
//\SILEX:todo

/**
 * List of authorized sorting values.
 * @see DocumentationAnnotation.EXAMPLE_SORTING_ALLOWABLE_VALUES
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum SortingValues {
    
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
