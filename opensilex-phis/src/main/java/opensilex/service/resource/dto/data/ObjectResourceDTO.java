//******************************************************************************
//                                ObjectResourceDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 21 mai 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import java.util.ArrayList;
import java.util.List;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;

/**
 * Resource DTO for the objects in the Data services
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ObjectResourceDTO extends AbstractVerifiedClass {
    //uri of the rdf resource
    //@example http://www.opensilex.org/demo/id/provenance/1553100256084
    protected String uri;
    //Label of the rdf resource
    //@example PROV2019-3
    protected List<String> labels;

    public ObjectResourceDTO(String uri, List<String> labels) {
        this.uri = uri;
        this.labels = labels;
    }

    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }

    public void addLabel(String label) {
        this.labels.add(label);
    }
}
