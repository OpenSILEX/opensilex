/*
 * *****************************************************************************
 *                         SpiderMutagenModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 29/06/2026 16:52
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.dal;

import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.front.vueOwlExtension.VueOwlExtension;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.yvan.ontology.YvanOntology;

import java.util.List;

@SPARQLResource(
        ontology = YvanOntology.class,
        resource = "SpiderMutagen",
        graph = SpiderMutagenModel.GRAPH
)
public class SpiderMutagenModel extends SPARQLResourceModel {

    protected static final String GRAPH = "extension/yvan";

    @SPARQLProperty(
            ontology = YvanOntology.class,
            property = "legsNumber",
            required = true
    )
    private Integer legsNumber;

    @SPARQLProperty(
            ontology = YvanOntology.class,
            property = "linkedDevice"
    )
    private List<DeviceModel> linkedDevices;

    @SPARQLProperty(
            ontology = VueOwlExtension.class,
            property = "hasIcon"
    )
    private String hasIcon;


    public Integer getLegsNumber() {
        return legsNumber;
    }

    public void setLegsNumber(Integer legsNumber) {
        this.legsNumber = legsNumber;
    }

    public List<DeviceModel> getLinkedDevices() {
        return linkedDevices;
    }

    public void setLinkedDevices(List<DeviceModel> linkedDevices) {
        this.linkedDevices = linkedDevices;
    }

    public String getHasIcon() {
        return hasIcon;
    }

    public void setHasIcon(String hasIcon) {
        this.hasIcon = hasIcon;
    }
}
