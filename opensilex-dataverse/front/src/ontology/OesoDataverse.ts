let OESO_DATAVERSE_URI = "http://www.opensilex.org/vocabulary/oeso-dataverse";
let OESO_DATAVERSE_PREFIX = "vocabulary-dataverse";

let getShortURI = (uri) => {
    return uri.replace(OESO_DATAVERSE_URI + "#", OESO_DATAVERSE_PREFIX + ":")
}

let checkURIs = (uri1, uri2) => {
    return getShortURI(uri1) == getShortURI(uri2);
}

let ontologies = {
    URI: OESO_DATAVERSE_URI,
    RECHERCHE_DATA_GOUV_DATASET_TYPE_URI: OESO_DATAVERSE_URI + "#RechercheDataGouvDataset",

    getShortURI: getShortURI,
    checkURIs: checkURIs
};

export default ontologies;