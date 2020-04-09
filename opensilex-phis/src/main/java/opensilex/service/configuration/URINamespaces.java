//******************************************************************************
//                            URINamespaces.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 6 Aug. 2017
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Specific namespaces.
 * @update [Morgane Vidal] 12 Sept. 2018: remove ontology references (moved in 
 * the ontology package)
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class URINamespaces {
    
    // Add additionnal custom users namespaces to triplestore based namespace. e.g. platform => http://www.phenome-fppn.fr/platform/
    // These ones override the namespaces found in rdf4j 
    /** @see allPaginateNamespacesProperties function for usage **/
    // For example if "platform" namespace is defined as http://www.phenome-fppn.fr/platform in rdf4j, this static function 
    // below will allow to override "plateform" in http://www.phenome-fppn.fr/ephesia 
    public static final Map<String, String> USER_SPECIFIC_NAMESPACES;
    
    static {
        Map<String, String> temporaryMap = new HashMap<>();
        //SILEX:info
        // Uncomment if you want to add other namespace
        // String plateform = PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "infrastructure");
        // Can put multiple other namespaces
        // temporaryMap.put("platform", "http://www.phenome-fppn.fr/" + plateform);
        //\SILEX:info
        USER_SPECIFIC_NAMESPACES = Collections.unmodifiableMap(temporaryMap);
    }
}