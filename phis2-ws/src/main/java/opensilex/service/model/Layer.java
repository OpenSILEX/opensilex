//******************************************************************************
//                                  Layer.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: August 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.HashMap;

/**
 * Layer model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Layer {
    
    /**
     * URI of the object the layer corresponds to.
     */
    private String objectURI;
    
    /**
     * Object type.
     * @example http://www.opensilex.org/vocabulary/oeso/Experiment
     */
    private String objectType;
    
    /**
     * True if the layer has all its corresponding object's descendants, false 
     * if it has only its direct children.
     */
    private String depth;
    
    /**
     * Path of the GeoSON file corresponding to the layer.
     */
    private String filePath;
    
    /**
     * The corresponding object's descendants. Key: URI, value: type.
     */
    private HashMap<String,String> children; 

    public String getObjectURI() {
        return objectURI;
    }

    public void setObjectURI(String objectURI) {
        this.objectURI = objectURI;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public void addChildren(String key, String value) {
        this.children.put(key, value);
    }
    
    public HashMap<String, String> getChildren() {
        return this.children;
    }
}
