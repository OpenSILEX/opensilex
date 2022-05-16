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
    PROJECT_TYPE_URI: OESO_URI + "#Project",
    EXPERIMENT_TYPE_URI: OESO_URI + "#Experiment",
    SCIENTIFIC_OBJECT_TYPE_URI: OESO_URI + "#ScientificObject",
    FACILITY_TYPE_URI: OESO_URI + "#Facility",
    GERMPLASM_TYPE_URI: OESO_URI + "#Germplasm",
    SPECIES_TYPE_URI: OESO_URI + "#Species",
    VARIETY_TYPE_URI: OESO_URI + "#Variety",
    ACCESSION_TYPE_URI: OESO_URI + "#Accession",
    PLANT_MATERIAL_LOT_TYPE_URI: OESO_URI + "#PlantMaterialLot",
    HAS_EXPERIMENT_MODALITIES: OESO_URI + "#hasExperimentModalities",
    HAS_CREATION_DATE: OESO_URI + "#hasCreationDate",
    HAS_DESTRUCTION_DATE: OESO_URI + "#hasDestructionDate",
    HAS_REPLICATION: OESO_URI + "#hasReplication",
    HAS_VARIETY: OESO_URI + "#hasVariety",
    HAS_SPECIES: OESO_URI + "#hasSpecies",
    IS_HOSTED: OESO_URI + "#isHosted",
    ENTITY_TYPE_URI: OESO_URI + "#Entity",
    QUALITY_TYPE_URI: OESO_URI + "#Quality",
    METHOD_TYPE_URI: OESO_URI + "#Method",
    UNIT_TYPE_URI: OESO_URI + "#Unit",
    DOCUMENT_TYPE_URI: OESO_URI + "#Document",
    CONCERNS: OESO_URI + "#concerns",
    AREA_TYPE_URI: OESO_URI + "#Area",
    STRUCTURAL_AREA_TYPE_URI: OESO_URI + "#StructuralArea",
    OPERATOR_TYPE_URI: OESO_URI + "#Operator",
    SENSOR_TYPE_URI: OESO_URI + "#SensingDevice",
    VECTOR_TYPE_URI: OESO_URI + "#Vector",
    SOFTWARE_TYPE_URI: OESO_URI + "#Software",
    DEVICE_TYPE_URI: OESO_URI + '#Device',
    IS_PART_OF: OESO_URI + "#isPartOf",
    IMAGE_TYPE_URI: OESO_URI + "#Image",
    DATAFILE_TYPE_URI: OESO_URI + "#Datafile",
    FACTOR_CATEGORY_URI: OESO_URI + '#FactorCategory',
    MEASURES_PROP_URI: OESO_URI + "#measures",
    VARIABLESGROUP_TYPE_URI: OESO_URI + "#VariablesGroup",
    getShortURI: getShortURI,
    checkURIs: checkURIs
};

export default ontologies;