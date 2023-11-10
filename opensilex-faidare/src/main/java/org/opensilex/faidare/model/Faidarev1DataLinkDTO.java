//******************************************************************************
//                          Faidarev1DataLinkDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1DataLinkDTO {
    private String dataLinkName;
    private String name;
    private String type;
    private String url;

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
