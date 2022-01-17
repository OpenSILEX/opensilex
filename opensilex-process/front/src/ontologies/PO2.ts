let PO2_URI = "http://opendata.inra.fr/PO2/";
let PO2_PREFIX = "PO2";

let getShortURI = (uri) => {
    return uri.replace(PO2_URI + "/", PO2_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: PO2_URI,
    TRANSFORMATION_PROCESS_URI: PO2_URI + "/Transformation_Process",
    STEP_URI: PO2_URI + "/step",
    HAS_FOR_STEP_URI: PO2_URI + "/hasForStep",
    HAS_INPUT_URI: PO2_URI + "/hasInput",
    HAS_OUTPUT_URI: PO2_URI + "/hasOutput",
    IS_COMPOSED_OF_URI: PO2_URI + "/isComposedOf",
    getShortURI: getShortURI,
    checkURIs: checkURIs
};

export default ontologies;