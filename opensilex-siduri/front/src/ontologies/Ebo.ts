let EBO_URI = "http://opendata.inrae.fr/ebo";
let EBO_PREFIX = "ebo";

let getShortURI = (uri) => {
    return uri.replace(EBO_URI + "#", EBO_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: EBO_URI,
    getShortURI: getShortURI,
    checkURIs: checkURIs
};

export default ontologies;