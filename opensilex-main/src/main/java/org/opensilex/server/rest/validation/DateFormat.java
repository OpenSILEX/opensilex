//******************************************************************************
//                              DateFormat.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

/**
 * List of authorized date formats.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public enum DateFormat {
    YMDHMSZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd HH:mm:ssZ";
        }
    },
    YMDTHMS {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ss";
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
    },
    // DMY {
    //     @Override
    //     public String toString(){
    //         return "dd-MM-yyyy";
    //     }
    // },
    YMDTHMSMS {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ss.SSS";
        }
    },
    YMDTHMSMSZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        }
    },
    YMDTHMSMSZZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
        }
    },
    YMDTHMSMSX {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ss.SSSXXXX";
        }
    },
    YMDTHMSMSGTZ {
        @Override
        public String toString(){
            return "yyyy-MM-dd'T'HH:mm:ss.SSSz";
        }
    };
}
