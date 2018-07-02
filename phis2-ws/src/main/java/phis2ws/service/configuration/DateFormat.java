//******************************************************************************
//                                       DateFormat.java
//
// Author(s): Arnaud Charleroy<arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 29 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  29 juin 2018
// Subject:
//******************************************************************************
package phis2ws.service.configuration;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public enum DateFormat {
    
    YMDHMSZ{
        @Override
        public String toString(){
            return "yyyy-MM-dd HH:mm:ssZ";
        }
    },
    YMDTHMSZ{
        @Override
        public String toString(){
            return "yyyy-MM-ddTHH:mm:ssZ";
        }
    } ,
    YMD{
        @Override
        public String toString(){
            return "yyyy-MM-dd";
        }
    }
    
}
