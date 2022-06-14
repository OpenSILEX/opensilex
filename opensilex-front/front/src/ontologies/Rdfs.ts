let RDFS_URI = "http://www.w3.org/2000/01/rdf-schema";
let RDFS_PREFIX = "rdfs";

let getShortURI = (uri) => {
    return uri.replace(RDFS_URI + "#", RDFS_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: RDFS_URI,
    COMMENT: RDFS_URI + "#comment",
    LABEL: RDFS_URI + "#label",
    getShortURI,
    checkURIs
};


export default ontologies;