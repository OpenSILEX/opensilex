//******************************************************************************
//                                 Contexts.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

import opensilex.service.PropertiesFileManager;

/**
 * Triplestore contexts.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Contexts {
    
    
    //The actuators context contains all the declared actuators.
    ACTUATORS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/actuators";
        }
    },
    //The context which contains all the declared annotations
    ANNOTATIONS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/annotation";
        }
    },
    //The context which contains all the declared documents metadata
    DOCUMENTS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "documents";
        }
    },
    //The context which contains all the declared events
    EVENTS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/events";
        }
    },
    //The context which contains all the declared infrastructures
    INFRASTRUCTURES {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/infrastructure";
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
    //The context which contains all the declared projects
    PROJECTS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/projects";
        }
    },
    //The context which contains all the declared radiometric targets
    RADIOMETRIC_TARGETS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "set/radiometricTargets";
        }
    },
    //The context which contains all the declared scientific objects
    SCIENTIFIC_OBJECTS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "scientificObjects";
        }
    },
    //The context which contains all the declared sensors
    SENSORS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "sensors";
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
    },
    //The context which contains the germplasm
    GENETIC_RESOURCE {
        @Override
        public String toString() {
            return PLATFORM.toString() + "geneticResources"; 
        }
    },
    //The context which contains the germplasm
    SPECIES {
        @Override
        public String toString() {
            return PLATFORM.toString() + "species"; 
        }
    },
    //The context which contains the germplasm
    VARIETY {
        @Override
        public String toString() {
            return PLATFORM.toString() + "variety"; 
        }
    },
    //The context which contains the germplasm
    ACCESSION {
        @Override
        public String toString() {
            return PLATFORM.toString() + "accessions"; 
        }
    },
    //The context which contains the germplasm
    GENUS {
        @Override
        public String toString() {
            return PLATFORM.toString() + "genus"; 
        }
    },
    //The context which contains the germplasm
    PLANT_MATERIAL_LOT {
        @Override
        public String toString() {
            return PLATFORM.toString() + "plantMaterialLot"; 
        }
    }
}

