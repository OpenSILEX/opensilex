let TIME_URI = "http://www.w3.org/2006/time";
let TIME_PREFIX = "time";

let getShortURI = (uri) => {
    return uri.replace(TIME_URI + "#", TIME_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: TIME_URI,
    HAS_BEGINNING: TIME_URI + "#hasBeginning",
    HAS_END: TIME_URI + "#hasEnd",
    getShortURI,
    checkURIs
};


export default ontologies;