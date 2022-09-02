let OEEV_URI = "http://www.opensilex.org/vocabulary/oeev";
let MOVE =  "Move";
let OEEV_PREFIX = "oeev";

let getShortURI = (uri) => {
    return uri.replace(OEEV_URI + "#", OEEV_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: OEEV_URI,
    EVENT_TYPE_URI: OEEV_URI + "#Event",
    MOVE_TYPE_URI: OEEV_URI + "#Move",
    CALIBRATION_TYPE_URI: OEEV_URI + "#Calibration",

    CONCERNS: OEEV_URI + "#concerns",
    IS_INSTANT: OEEV_URI + "#isInstant",
    FROM: OEEV_URI + "#from",
    TO: OEEV_URI + "#to",
    getShortURI,
    checkURIs
};


export default ontologies;