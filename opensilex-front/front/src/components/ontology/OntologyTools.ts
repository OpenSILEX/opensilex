import {VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../lib";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";

interface NameAndUri{
    name: string,
    uri: string
}

/**
 * @param properties properties to sort according {@link typeModel} {@link VueRDFTypeDTO#properties_order}
 * @param typeModel the type from which the properties originate from
 * @param opensilex opensilexVuePlugin
 * @return sorted properties if {@link typeModel} has a non null or empty {@link VueRDFTypeDTO#properties_order}, return properties else
 * Precondition : typeModel has a properties order
 */
export function sortProperties(properties: Array<VueRDFTypePropertyDTO>, typeModel: VueRDFTypeDTO, opensilex: OpenSilexVuePlugin): Array<VueRDFTypePropertyDTO>{

    //Create property-index map from the properties_order list to use during the sort
    let propertyToOrderIndexMap : Map<string, number> = new Map<string, number>();

    for(let orderIndex = 0; orderIndex < typeModel.properties_order.length; orderIndex++){
        propertyToOrderIndexMap.set(typeModel.properties_order[orderIndex], orderIndex);
    }
    return properties.sort((propModel1, propModel2) => {
        let property1 = propModel1.uri;
        let property2 = propModel2.uri;
        if (property1 === property2) {
            return 0;
        }

        // always put name (rdfs:label) in first
        if (opensilex.checkURIs(property1, opensilex.Rdfs.LABEL)) {
            return -1;
        }

        if (opensilex.checkURIs(property2, opensilex.Rdfs.LABEL)) {
            return 1;
        }

        let aIndex = propertyToOrderIndexMap.get(property1);
        let bIndex = propertyToOrderIndexMap.get(property2);

        if (aIndex === -1) {
            if (bIndex === -1) {
                return property1.localeCompare(property2);
            } else {
                return -1;
            }
        } else {
            if (bIndex === -1) {
                return 1;
            } else {
                return aIndex - bIndex;
            }
        }
    });
}

/**
 * Creates a list of every property uri that appears in a list of ResourceTreeDTOs, ordered by name
 */
export function createUriListFromGetPropertiesResult(propertyList : Array<ResourceTreeDTO>, opensilex: OpenSilexVuePlugin) : Array<string>{
    let nameUriUnorderedList: Array<NameAndUri> = createNameUriListFromGetPropertiesResult(propertyList, opensilex);
    let nameUriOrderedList: Array<NameAndUri> = nameUriUnorderedList.sort((a, b) => {
        if(!a.name){
            return 1;
        }
        if(!b.name){
            return -1;
        }
        return a.name.localeCompare(b.name);
    });
    return nameUriOrderedList.map(e => e.uri);
}

/**
 * Recursive function to look in children, children of children, etc...
 * To create a list of every {uri, name} of every property that appears in an array of ResourceTreeDTOs
 */
function createNameUriListFromGetPropertiesResult(propertyList : Array<ResourceTreeDTO>, opensilex: OpenSilexVuePlugin) : Array<NameAndUri>{
    let result = [];
    propertyList.forEach(e => {
        result.push({uri: opensilex.getShortUri(e.uri), name: e.name});
        if(e.children && e.children.length > 0){
            result.push(...createNameUriListFromGetPropertiesResult(e.children, opensilex));
        }
    });
    return result;
}
