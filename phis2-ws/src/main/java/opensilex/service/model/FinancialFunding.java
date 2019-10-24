/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.model;

/**
 *
 * @author vincent
 */
public class FinancialFunding {

    /**
     * Instance URI.
     *
     * @example http://www.opensilex.org/vocabulary/oeso#internationalfunding
     */
    protected String uri;

    /**
     * The rdfs:label of the instance.
     *
     * @example International
     */
    protected String label;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    

}
