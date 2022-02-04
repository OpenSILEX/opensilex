let FOAF_URI = "http://xmlns.com/foaf/0.1/";
let FOAF_PREFIX = "foaf";

let getShortUri = (uri) => {
    return uri.replace(FOAF_URI, FOAF_PREFIX + ":");
};

let checkURIS = (uri1, uri2) => {
    return getShortUri(uri1) === getShortUri(uri2);
};

let ontologies = {
    URI: FOAF_URI,
    ORGANIZATION_TYPE_URI: FOAF_URI + "Organization",
    getShortUri,
    checkURIS
};

export default ontologies;