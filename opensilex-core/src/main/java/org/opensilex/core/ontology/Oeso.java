//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 * @author Vincent MIGOT
 */
public class Oeso {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/oeso";

    public static final String PREFIX = "vocabulary";

    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = DOMAIN + "#";

    /**
     * The namespace of the vocabulary as a string
     *
     * @return namespace as String
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }

    /**
     * Vocabulary namespace
     */
    public static final Resource NAMESPACE = Ontology.resource(NS);

    public static final Property longString = Ontology.property(NS, "longString");

    // ---- COMMON PROPERTIES ----
    public static final Property startDate = Ontology.property(NS, "startDate");
    public static final Property endDate = Ontology.property(NS, "endDate");
    public static final Property hasPart = Ontology.property(NS, "hasPart");

    // ---- VARIABLES ----
    public static final Resource Variable = Ontology.resource(NS, "Variable");
    public static final Resource Entity = Ontology.resource(NS, "Entity");
    public static final Resource Characteristic = Ontology.resource(NS, "Characteristic");
    public static final Resource Method = Ontology.resource(NS, "Method");
    public static final Resource Unit = Ontology.resource(NS, "Unit");

    public static final Property hasEntity = Ontology.property(NS, "hasEntity");
    public static final Property hasCharacteristic = Ontology.property(NS, "hasCharacteristic");
    public static final Property hasTraitUri = Ontology.property(NS, "hasTraitUri");
    public static final Property hasTraitName = Ontology.property(NS, "hasTraitName");

    public static final Property hasMethod = Ontology.property(NS, "hasMethod");
    public static final Property hasUnit = Ontology.property(NS, "hasUnit");
    public static final Property hasDataType = Ontology.property(NS, "hasDataType");

    // ---- VARIABLES Unit ----
    public static final Property hasTimeInterval = Ontology.property(NS, "hasTimeInterval");
    public static final Property hasSamplingInterval = Ontology.property(NS, "hasSamplingInterval");
    public static final Property hasSymbol = Ontology.property(NS, "hasSymbol");
    public static final Property hasAlternativeSymbol = Ontology.property(NS, "hasAlternativeSymbol");
    
    // ---- VARIABLES GROUP ----
    public static final Resource VariablesGroup = Ontology.resource(NS, "VariablesGroup");
    public static final Property hasVariable = Ontology.property(NS, "hasVariable");
    
    // ----- USERS ------
    public static final Resource ScientificSupervisor = Ontology.resource(NS, "ScientificSupervisor");
    public static final Resource TechnicalSupervisor = Ontology.resource(NS, "TechnicalSupervisor");

    // ---- PROJECTS ----
    public static final Resource Project = Ontology.resource(NS, "Project");

    public static final Property hasShortname = Ontology.property(NS, "hasShortname");
    public static final Property hasObjective = Ontology.property(NS, "hasObjective");
    public static final Property hasAdministrativeContact = Ontology.property(NS, "hasAdministrativeContact");
    public static final Property hasCoordinator = Ontology.property(NS, "hasCoordinator");
    public static final Property hasScientificContact = Ontology.property(NS, "hasScientificContact");
    public static final Property hasRelatedProject = Ontology.property(NS, "hasRelatedProject");
    public static final Property hasFinancialFunding = Ontology.property(NS, "hasFinancialFunding");

    // ---- EXPERIMENTS ----
    public static final Resource Experiment = Ontology.resource(NS, "Experiment");

    public static final Property hasDevice = Ontology.property(NS, "hasDevice");
    public static final Property hasInfrastructure = Ontology.property(NS, "hasInfrastructure");
    public static final Property hasProject = Ontology.property(NS, "hasProject");
    public static final Property hasScientificSupervisor = Ontology.property(NS, "hasScientificSupervisor");
    public static final Property hasTechnicalSupervisor = Ontology.property(NS, "hasTechnicalSupervisor");
    public static final Property hasCampaign = Ontology.property(NS, "hasCampaign");
    public static final Property hasSpecies = Ontology.property(NS, "hasSpecies");
    public static final Property isPublic = Ontology.property(NS, "isPublic");
    public static final Property measures = Ontology.property(NS, "measures");
    public static final Property participatesIn = Ontology.property(NS, "participatesIn");

    // ---- INFRASTRUCTURES AND INSTALLATION
    public static final Resource Infrastructure = Ontology.resource(NS, "Infrastructure");
    public static final Resource InfrastructureFacility = Ontology.resource(NS, "InfrastructureFacility");
    public static final Resource InfrastructureTeam = Ontology.resource(NS, "InfrastructureTeam");
    public static final Resource Installation = Ontology.resource(NS, "Installation");
    public static final Property hasFacility = Ontology.property(NS, "hasFacility");

    // ---- SPECIES ----
    // public static final Resource Species = Ontology.resource(NS, "Species");
    // ---- FACTORS ----
    public static final Resource Factor = Ontology.resource(NS, "Factor");
    public static final Resource FactorLevel = Ontology.resource(NS, "FactorLevel");
    public static final Property hasFactorLevel = Ontology.property(NS, "hasFactorLevel");
    public static final Property hasFactor = Ontology.property(NS, "hasFactor");
    public static final Property hasCategory = Ontology.property(NS, "hasCategory");
    // Link with experiment
    public static final Property studyEffectOf = Ontology.property(NS, "studyEffectOf");
    public static final Property studiedEffectIn = Ontology.property(NS, "studiedEffectIn");

    public static final Resource SensingDevice = Ontology.resource(NS, "SensingDevice");
    public static final Resource Operator = Ontology.resource(NS, "Operator");

    // ---- GERMPLASM ----
    public static final Resource Germplasm = Ontology.resource(NS, "Germplasm");
    public static final Resource Species = Ontology.resource(NS, "Species");
    public static final Resource Variety = Ontology.resource(NS, "Variety");
    public static final Resource Accession = Ontology.resource(NS, "Accession");
    public static final Resource PlantMaterialLot = Ontology.resource(NS, "PlantMaterialLot");
    public static final Property fromSpecies = Ontology.property(NS, "fromSpecies");
    public static final Property fromVariety = Ontology.property(NS, "fromVariety");
    public static final Property fromAccession = Ontology.property(NS, "fromAccession");
    public static final Property fromInstitute = Ontology.property(NS, "fromInstitute");
    public static final Property hasProductionYear = Ontology.property(NS, "hasProductionYear");
    public static final Property hasGermplasm = Ontology.property(NS, "hasGermplasm");
    public static final Property hasId = Ontology.property(NS, "hasId");

    // ---- SCIENTIFIC OBJECTS ----
    public static final Resource ScientificObject = Ontology.resource(NS, "ScientificObject");
    public static final Property isPartOf = Ontology.property(NS, "isPartOf");
    public static final Resource ScientificObjectClass = Ontology.resource(NS, "ScientificObjectClass");
    public static final Property hasCreationDate = Ontology.property(NS, "hasCreationDate");
    public static final Property hasDestructionDate = Ontology.property(NS, "hasDestructionDate");

    // ---- AREA ----
    public static final Resource Area = Ontology.resource(NS, "Area");

    // ---- PROVENANCES ----
    public static final Property ImageAnalysis = Ontology.property(NS, "ImageAnalysis");
    public static final Property Image = Ontology.property(NS, "Image");

    // ---- DOCUMENTS ----
    public static final Resource Document = Ontology.resource(NS, "Document");
    public static final Property hasAuthor = Ontology.property(NS, "hasAuthor");
    public static final Resource Datafile = Ontology.resource(NS, "Datafile");
    
     // ---- DEVICES ----
    public static final Resource Device = Ontology.resource(NS, "Device");
    public static final Property hasBrand = Ontology.property(NS, "hasBrand");
    public static final Property hasSerialNumber = Ontology.property(NS,"hasSerialNumber");
    public static final Property hasModel = Ontology.property(NS,"hasModel");
    public static final Property personInCharge = Ontology.property(NS,"personInCharge");
    public static final Property startUp = Ontology.property(NS,"startUp");
    public static final Property removal = Ontology.property(NS,"removal");

}
