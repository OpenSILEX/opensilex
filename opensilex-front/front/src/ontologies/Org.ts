let ORG_URI = "http://www.w3.org/ns/org#";
let ORG_PREFIX = "org";

let getShortUri = (uri) => {
    return uri.replace(ORG_URI, ORG_PREFIX + ":");
};

let checkURIS = (uri1, uri2) => {
    return getShortUri(uri1) === getShortUri(uri2);
};

let ontologies = {
    URI: ORG_URI,
    SITE_TYPE_URI: ORG_URI + "Site",
    getShortUri,
    checkURIS
};

export default ontologies;