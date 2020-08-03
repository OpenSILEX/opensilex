let OESO_URI = "http://www.opensilex.org/vocabulary/oeso";

let ontologies = {
    URI: OESO_URI,
    EXPERIMENT_TYPE_URI: OESO_URI + "#Experiment",
    INFRASTRUCTURE_TYPE_URI: OESO_URI + "#Infrastructure",
    INFRASTRUCTURE_FACILITY_TYPE_URI: OESO_URI + "#InfrastructureFacility",
    SCIENTIFIC_OBJECT_TYPE_URI: OESO_URI + "#ScientificObject",
    GERMPLASM_TYPE_URI: OESO_URI + "#Germplasm",
    PLANT_MATERIAL_LOT_TYPE_URI: OESO_URI + "#PlantMaterialLot",
    HAS_EXPERIMENT_MODALITIES: OESO_URI + "#hasExperimentModalities",
    HAS_REPLICATION: OESO_URI + "#hasReplication",
    HAS_VARIETY: OESO_URI + "#hasVariety",
    HAS_SPECIES: OESO_URI + "#hasSpecies",
};

export default ontologies;