//******************************************************************************
//                          DataLink.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Alice Boizet, Bernhard Gschloessl
 */
public class DataLink {
    private String dataLinkName; //v1.3 specific
    private String name; //v2.1 : The name of the external data link MIAPPE V1.1 (DM-38) Data file description
    private String type; //v1.3 specific
    private String url; //v2.1 : URL describing the location of this data file to view or download MIAPPE V1.1 (DM-37) Data file link

    public String getDataLinkName() {
        return dataLinkName;
    }

    public void setDataLinkName(String dataLinkName) {
        this.dataLinkName = dataLinkName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
}
