//******************************************************************************
//                                 Data.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.Date;

/**
 * Data model
 * @auhor Vincent Migot <vincent.migot@inra.fr>
 */
public class Data {
    /**
     * URI
     * @example http://www.opensilex.org/1e9eb2fbacc7222d3868ae96149a8a16b32b2a1870c67d753376381ebcbb5937/e78da502-ee3f-42d3-828e-aa8cab237f93
     */
    protected String uri;
    
    /**
     * Provenance URI from which data comes.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String provenanceUri;
    
    /**
     * Related scientific object URI.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String objectUri;
    
    /**
     * Measured variable URI.
     * @example http://www.phenome-fppn.fr/mtp/id/variables/v002
     */
    protected String variableUri;
    
    /**
     * Date of the value. The format should be yyyy-MM-ddTHH:mm:ssZ
     * @example 2018-06-25T15:13:59+0200
     */
    protected Date date;
    
    /** 
     * The measured value.
     * @example 1.2
     */
    protected Object value;

    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getProvenanceUri() {
        return provenanceUri;
    }

    public void setProvenanceUri(String provenanceUri) {
        this.provenanceUri = provenanceUri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
