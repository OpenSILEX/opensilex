//******************************************************************************
//                               Triplet.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 Mar. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Triplet model.
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Triplet {
    
    /**
     * Subject of the triplet.
     */
    private String subject;
    
    /**
     * Property of the triplet.
     * Corresponds to the relation between the subject and the object.
     */
    private String property;
    
    /**
     * Object of the triplet.
     */
    private String object;
    
    /**
     * Object type. It can a literal or a URI.
     */
    private String objectType;
    
    /**
     * Object language. 
     * @example en-US
     */
    private String objectLang;
    
    /**
     * Graph URI of the triplet.
     */
    private String graph;

    public Triplet() {
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectLang() {
        return objectLang;
    }

    public void setObjectLang(String objectLang) {
        this.objectLang = objectLang;
    }

    public String getGraph() {
        return graph;
    }

    public void setG(String graph) {
        this.graph = graph;
    }
}
