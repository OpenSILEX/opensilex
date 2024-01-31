package org.opensilex.front.api;

import java.net.URI;

public class GraphConfigDTO {

    private URI variable;

    public URI getVariable() {
        return variable;
    }

    public void setVariable(URI variable) {
        this.variable = variable;
    }

    public String dataLocationInformations;

    public String  getDataLocationInformations() {
        return dataLocationInformations;
    }

    public void setDataLocationInformations(String dataLocationInformations) {
        this.dataLocationInformations = dataLocationInformations;
    }

}
