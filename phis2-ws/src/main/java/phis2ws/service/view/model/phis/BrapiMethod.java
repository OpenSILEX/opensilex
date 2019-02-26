//******************************************************************************
//                                       BrapiMethod.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a method according to brapi specifications
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiMethod {
    //The id of the method in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/methods/m001
    private String methodDbId;
    //The label of the method in the triplestore e.g. "LAI_Computation"
    private String methodName;
    @SerializedName("class")
    private String brapiClass;
    private String description;
    //The comment of the method in the triplestore
    private String formula;
    //SILEX:todo
    //Create a brapiOntologyReference class to follow this format
    //    "ontologyReference": {
    //            "documentationLinks": [
    //              {
    //                "URL": "https://ontology.org/t1",
    //                "type": "WEBPAGE",
    //                "url": "https://ontology.org/t1"
    //              }
    //            ],
    //            "ontologyDbId": "MO_123",
    //            "ontologyName": "Ontology.org",
    //            "version": "17"
    //          }
    //and see if we can use the ontologyReference of variableDAO to fill the attributes 
    //but in the case where there are several references, which one should we get ?
    private String ontologyReference;
    //\SILEX
    private String reference;

    public BrapiMethod() {
    }

    public String getMethodDbId() {
        return methodDbId;
    }

    public void setMethodDbId(String methodDbId) {
        this.methodDbId = methodDbId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodname) {
        this.methodName = methodname;
    }

    public String getBrapiClass() {
        return brapiClass;
    }

    public void setBrapiClass(String classe) {
        this.brapiClass = classe;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getOntologyReference() {
        return ontologyReference;
    }

    public void setOntologyReference(String ontologyReference) {
        this.ontologyReference = ontologyReference;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }  
}
