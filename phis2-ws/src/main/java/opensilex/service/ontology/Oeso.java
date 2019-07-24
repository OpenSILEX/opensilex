//******************************************************************************
//                              Vocabulary.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

import opensilex.service.PropertiesFileManager;

/**
 * Vocabulary definition from the OESO ontology.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Oeso {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.opensilex.org/vocabulary/oeso#";
        }
    },
    
    CONCEPT_SCIENTIFIC_OBJECT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "ScientificObject";
        }
    },
    CONCEPT_ACTUATOR {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Actuator";
        }
    },
    CONCEPT_ANNOTATION {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Annotation";
        }
    },
    CONCEPT_CAMERA {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Camera";
        }
    },
    CONCEPT_DEVICE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Device";
        }
    },
    CONCEPT_DOCUMENT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Document";
        }
    },
    CONCEPT_EXPERIMENT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Experiment";
        }
    },
    CONCEPT_FIELD_ROBOT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "FieldRobot";
        }
    },
    CONCEPT_GENOTYPE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Genotype";
        }
    },
    CONCEPT_INFRASTRUCTURE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Infrastructure";
        }
    },
    CONCEPT_IMAGE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Image";
        }
    },
    CONCEPT_METHOD {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Method";
        }
    },
    CONCEPT_PROJECT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Project";
        }
    },
    CONCEPT_PROVENANCE {
       @Override
        public String toString() {
            return NAMESPACE.toString() + "Provenance";
        } 
    },
    CONCEPT_RADIOMETRIC_TARGET {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "RadiometricTarget";
        }
    },
    CONCEPT_SENSING_DEVICE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "SensingDevice";
        }
    },
    CONCEPT_SPECIES {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Species";
        }
    },
    CONCEPT_TRAIT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Trait";
        }
    },
    CONCEPT_UAV {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "UAV";
        }
    },
    CONCEPT_UNIT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Unit";
        }
    },
    CONCEPT_VARIABLE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Variable";
        }
    },
    CONCEPT_VARIETY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Variety";
        }
    },
    CONCEPT_VECTOR {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Vector";
        }
    },
    
    PLATFORM_URI {
        @Override
        public String toString() {
            return PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "baseURI") 
                    + PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "infrastructure");
        }
    },
    
    RELATION_BRDF_P1 {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "brdfP1";
        }
    },
    RELATION_BRDF_P2 {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "brdfP2";
        }
    },
    RELATION_BRDF_P3 {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "brdfP3";
        }
    },
    RELATION_BRDF_P4 {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "brdfP4";
        }
    },
    RELATION_CONCERNS {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "concerns";
        }
    },
    RELATION_DATE_OF_LAST_CALIBRATION {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "dateOfLastCalibration";
        }
    },
    RELATION_DATE_OF_PURCHASE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "dateOfPurchase";
        }
    },
    RELATION_DEVICE_PROPERTY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "deviceProperty";
        }
    },
    RELATION_END_DATE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "endDate";
        }
    },
    RELATION_FROM_GENOTYPE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "fromGenotype";
        }
    },
    RELATION_HAS_RELATED_PROJECT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasRelatedProject";
        }
    },
    RELATION_HAS_SPECIES {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasSpecies";
        }
    },
    RELATION_HAS_VARIETY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasVariety";
        }
    },
    RELATION_HAS_ALIAS {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasAlias";
        }
    },
    RELATION_HAS_BRAND {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasBrand";
        }
    },
    RELATION_HAS_CONTACT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasContact";
        }
    },
    RELATION_HAS_EXPERIMENT_MODALITIES {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasExperimentModalities";
        }
    },
    RELATION_HAS_FINANCIAL_REFERENCE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasFinancialReference";
        }
    },
    RELATION_HAS_FINANCIAL_FUNDING {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasFinancialFunding";
        }
    },
    RELATION_HAS_KEYWORD {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasKeyword";
        } 
    },
    RELATION_HAS_METHOD {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasMethod";
        }
    },
    RELATION_HAS_MODEL {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasModel";
        }
    },
    RELATION_HAS_OBJECTIVE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasObjective";
        }
    },
    RELATION_HAS_PLOT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasPlot";
        }
    },
    RELATION_HAS_RADIOMETRIC_TARGET_MATERIAL{
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasRadiometricTargetMaterial";
        }
    },
    RELATION_HAS_REPLICATION {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasReplication";
        }
    },
    RELATION_HAS_SHAPE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasShape";
        }
    },
    RELATION_HAS_SHAPE_DIAMETER {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasShapeDiameter";
        }
    },
    RELATION_HAS_SHAPE_LENGTH {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasShapeLength";
        }
    },
    RELATION_HAS_SHAPE_WIDTH {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasShapeWidth";
        }
    },
    RELATION_HAS_SHORTNAME {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasShortname";
        }
    },
    RELATION_HAS_TECHNICAL_CONTACT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasTechnicalContact";
        }
    },
    RELATION_HAS_TRAIT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasTrait";
        }
    },
    RELATION_HAS_UNIT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasUnit";
        }
    },
    RELATION_IN_SERVICE_DATE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "inServiceDate";
        }
    },
    RELATION_HAS_ADMINISTRATIVE_CONTACT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasAdministrativeContact";
        }
    },
    RELATION_HAS_COORDINATOR {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasCoordinator";
        }
    },
    RELATION_IS_PART_OF {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "isPartOf";
        }
    },
    RELATION_HAS_SCIENTIFIC_CONTACT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasScientificContact";
        }
    },
    RELATION_MEASURES {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "measures";
        }
    },
    RELATION_PARTICIPATES_IN {
       @Override
        public String toString() {
            return NAMESPACE.toString() + "participatesIn";
        } 
    },
    RELATION_PERSON_IN_CHARGE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "personInCharge";
        }
    },
    RELATION_HAS_SERIAL_NUMBER {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasSerialNumber";
        }
    },
    RELATION_START_DATE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "startDate";
        }
    },
    RELATION_STATUS {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "status";
        }
    },
    CONCEPT_DATA {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Data";
        }
    },
    CONCEPT_DATA_FILE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "DataFile";
        }
    };
}
