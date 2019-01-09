//******************************************************************************
//                                       Contexts.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

import phis2ws.service.PropertiesFileManager;

/**
 * The contexts used in SILEX-PHIS
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Contexts {
    //The context which contains all the declared scientific objects
    SCIENTIFIC_OBJECTS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "scientificObjects";
        }
    },
    //The context which contains all the declared annotations
    ANNOTATIONS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/annotation";
        }
    },
    //The context which contains all the declared radiometric targets
    RADIOMETRIC_TARGETS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/radiometricTargets";
        }
    },
    //The context which contains all the declared infrastructures
    INFRASTRUCTURES {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/infrastructure";
        }
    },
    //The context which contains all the declared sensors
    SENSORS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "sensors";
        }
    },
    //The context which contains all the declared documents metadata
    DOCUMENTS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "documents";
        }
    },
    //The context platform. 
    //The value is equals to the baseURI config param of the service.properties file.
    PLATFORM {
        @Override
        public String toString() {
            return PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "baseURI") 
                    + PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "infrastructure") + "/";
        }
    },
    //The context which contains the variables, traits, methods and units.
    VARIABLES {
        @Override
        public String toString() {
            return PLATFORM.toString() + "variables";
        }
    },
    //The context which contains the vectors.
    VECTORS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "vectors";
        }
    },
    //The context which contains the vocabulary ontology
    VOCABULARY {
        @Override
        public String toString() {
            return PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "vocabularyContext");
        }
    }
}

