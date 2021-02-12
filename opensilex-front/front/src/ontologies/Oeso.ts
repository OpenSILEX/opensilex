let OESO_URI = "http://www.opensilex.org/vocabulary/oeso";
let OESO_PREFIX = "vocabulary";

let getShortURI = (uri) => {
    return uri.replace(OESO_URI + "#", OESO_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: OESO_URI,
    EXPERIMENT_TYPE_URI: OESO_URI + "#Experiment",
    INFRASTRUCTURE_TYPE_URI: OESO_URI + "#Infrastructure",
    SCIENTIFIC_OBJECT_TYPE_URI: OESO_URI + "#ScientificObject",
    INFRASTRUCTURE_FACILITY_TYPE_URI: OESO_URI + "#InfrastructureFacility",
    GERMPLASM_TYPE_URI: OESO_URI + "#Germplasm",
    PLANT_MATERIAL_LOT_TYPE_URI: OESO_URI + "#PlantMaterialLot",
    HAS_EXPERIMENT_MODALITIES: OESO_URI + "#hasExperimentModalities",
    HAS_REPLICATION: OESO_URI + "#hasReplication",
    HAS_VARIETY: OESO_URI + "#hasVariety",
    HAS_SPECIES: OESO_URI + "#hasSpecies",
    ENTITY_TYPE_URI: OESO_URI + "#Entity",
    QUALITY_TYPE_URI: OESO_URI + "#Quality",
    METHOD_TYPE_URI: OESO_URI + "#Method",
    UNIT_TYPE_URI: OESO_URI + "#Unit",
    DOCUMENT_TYPE_URI: OESO_URI + "#Document",
    CONCERNS: OESO_URI + "#concerns",
    AREA_TYPE_URI: OESO_URI + "#Area",
    OPERATOR_TYPE_URI: OESO_URI + "#Operator",
    SENSOR_TYPE_URI: OESO_URI + "#SensorDevice",
    DEVICE_TYPE_URI: OESO_URI + '#Device',
    IS_PART_OF: OESO_URI + "#isPartOf",
    getShortURI: getShortURI,
    checkURIs: checkURIs
};

export default ontologies;