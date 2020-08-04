let OWL_URI = "http://www.w3.org/2002/07/owl";
let OWL_SHORT_URI = "owl";

let OWL = {
    URI: OWL_URI,
    SHORT_URI: OWL_SHORT_URI,
    DATATYPE_PROPERTY_URI: OWL_URI + "#DatatypeProperty",
    DATATYPE_PROPERTY_SHORT_URI: OWL_SHORT_URI + ":DatatypeProperty",
    OBJECT_PROPERTY_URI: OWL_URI + "#ObjectProperty",
    OBJECT_PROPERTY_SHORT_URI: OWL_SHORT_URI + ":ObjectProperty",

    isDatatypeProperty(typeURI) {
        return (typeURI == OWL.DATATYPE_PROPERTY_URI || typeURI == OWL.DATATYPE_PROPERTY_SHORT_URI);
    }
};

export default OWL;