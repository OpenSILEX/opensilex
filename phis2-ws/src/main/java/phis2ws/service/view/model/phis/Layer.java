//**********************************************************************************************
//                                       Layer.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: August 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  August, 28 2017
// Subject: Represents the layer
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.HashMap;

public class Layer {
    
    /**
     * @param objectURI URI de l'objet auquel la couche correspond
     * @param objectType type de l'objet (ex : http://www.opensilex.org/vocabulary/oeso/Experiment)
     * @param depth true si la couche a tous les descendants de objectURI
     *              false si on elle n'a que les enfants directs
     * @param filePath le chemin du fichier geoJSON correspondant à la couche
     * @param children la liste des descendants de objectURI. Clé : URI, valeur : type
     */
    private String objectURI;
    private String objectType;
    private String depth;
    private String filePath;
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
