//******************************************************************************
//                                       DateFormat.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug, 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.configuration;

//SILEX:todo
// Use this enum instead of the DateFormats class in all the application's code
//\SILEX:todo

/**
 * The list of the authorized date formats
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public enum DateFormat {
    YMDHMSZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd HH:mm:ssZ";
        }
    },
    YMDTHMSZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ssZ";
        }
    },
    YMDTHMSZZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ssZZ";
        }
    },
    YMD {
        @Override
        public String toString(){
            return "yyyy-MM-dd";
        }
    };
}
