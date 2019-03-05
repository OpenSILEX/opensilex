//******************************************************************************
//                          Data.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vincent.migotl@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.Base64;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.utils.UriGenerator;

/**
 * This is the model for the phenotypes data
 */
public class Data {

    private final static Logger LOGGER = LoggerFactory.getLogger(Data.class);
    
    //The uri of the provenance from which data come.
    //e.g. http://www.phenome-fppn.fr/mtp/2018/s18003
    protected String provenanceUri;
    //The uri of the scientific object on which data is related.
    //e.g. http://www.phenome-fppn.fr/mtp/2018/s18003
    protected String objectUri;
    //The uri of the measured variable
    //e.g. http://www.phenome-fppn.fr/mtp/id/variables/v002
    protected String variableUri;
    //The date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ
    //e.g. 2018-06-25T15:13:59+0200
    protected Date date;
    //The measured value.
    //e.g. 1.2
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
        String key  = getObjectUri() + getVariableUri() + getDate() + getProvenanceUri();
        
        try {
            return new UriGenerator().generateNewInstanceUri(Oeso.CONCEPT_DATA.toString(), null, key);
        } catch (Exception e) {
            LOGGER.error("Error while generating data URI, should never append", e);
            return Base64.getEncoder().encodeToString(key.getBytes());
        }
    }
}
