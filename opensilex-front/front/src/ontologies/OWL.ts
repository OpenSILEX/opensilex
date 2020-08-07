let OWL_URI = "http://www.w3.org/2002/07/owl";
let OWL_SHORT_URI = "owl";

let OWL = {
    URI: OWL_URI,
    SHORT_URI: OWL_SHORT_URI,
    DATATYPE_PROPERTY_URI: OWL_URI + "#DatatypeProperty",
    DATATYPE_PROPERTY_SHORT_URI: OWL_SHORT_URI + ":DatatypeProperty",
    TOP_DATA_PROPERTY_URI: OWL_URI + "#topDataProperty",
    TOP_DATA_PROPERTY_SHORT_URI: OWL_SHORT_URI + ":topDataProperty",
    OBJECT_PROPERTY_URI: OWL_URI + "#ObjectProperty",
    OBJECT_PROPERTY_SHORT_URI: OWL_SHORT_URI + ":ObjectProperty",
    TOP_OBJECT_PROPERTY_URI: OWL_URI + "#topObjectProperty",
    TOP_OBJECT_PROPERTY_SHORT_URI: OWL_SHORT_URI + ":topObjectProperty",
    isDatatypeProperty(typeURI) {
        return (typeURI == OWL.DATATYPE_PROPERTY_URI || typeURI == OWL.DATATYPE_PROPERTY_SHORT_URI);
    },
    isObjectTypeProperty(typeURI) {
        return (typeURI == OWL.OBJECT_PROPERTY_URI || typeURI == OWL.OBJECT_PROPERTY_SHORT_URI);
    },
    hasParent(parentURI) {
        if (!parentURI) {
            return false;
        }

        let isRootProperty = (parentURI == OWL.TOP_DATA_PROPERTY_URI
            || parentURI == OWL.TOP_DATA_PROPERTY_SHORT_URI
            || parentURI == OWL.TOP_OBJECT_PROPERTY_URI
            || parentURI == OWL.TOP_OBJECT_PROPERTY_SHORT_URI
        );

        return !isRootProperty;

    }
};

export default OWL;