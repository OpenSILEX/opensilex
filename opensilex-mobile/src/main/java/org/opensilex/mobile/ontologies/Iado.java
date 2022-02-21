//******************************************************************************
//                          Iado.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.ontologies;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class contains all the information concerning the Iado ontology
 *
 * @author Maximilian Hart
 */
public class Iado {
    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/iado";

    public static final String PREFIX = "vocabulary";

    /**
     * The namespace of the vocabulary as a string
     */
    private static final String NS = DOMAIN + "#";

    /**
     * The namespace of the vocabulary as a string
     *
     * @return namespace as String
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }

    //Form types:
    public static String redLiquid = NS + "RedLiquidWinemaking";
    public static String redSolid = NS + "RedSolidWinemaking";
    public static String harvest = NS + "Harvest";
    public static String white = NS + "WhiteWineMaking";
    public static String rose = NS + "RoseWinemaking";

    /**
     *
     * @param stringType
     * @return Uri of the given type (for csv imports)
     * @throws URISyntaxException
     */
    public static URI getProcessTypeFromCSVTemplate(String stringType) throws URISyntaxException {
        switch(stringType){
            case "RougeLiquide":
                return new URI(redLiquid);
            case "RougeSolide":
                return new URI(redSolid);
            case "Vendange":
                return new URI(harvest);
            case "Blanc":
                return new URI(white);
            case "Rose":
                return new URI(rose);
            default: return null;
        }
    }
}
