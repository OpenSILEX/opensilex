let OEEV_URI = "http://www.opensilex.org/vocabulary/oeev#";
let MOVE =  "Move";
let OEEV_PREFIX = "oeev";

let ontologies = {
    URI: OEEV_URI,
    EVENT_TYPE_URI: OEEV_URI + "Event",
    EVENT_TYPE_PREFIXED_URI: OEEV_PREFIX +":Event",
    MOVE_TYPE_URI: OEEV_URI + MOVE,
    MOVE_TYPE_PREFIXED_URI: OEEV_PREFIX +":" + MOVE,

    CONCERNS: OEEV_URI + "#concerns",
};


export default ontologies;