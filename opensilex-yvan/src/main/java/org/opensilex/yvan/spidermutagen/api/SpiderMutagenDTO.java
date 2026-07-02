/*
 * *****************************************************************************
 *                         SpiderMutageneDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 26/06/2026 17:17
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.yvan.spidermutagen.dal.SpiderMutagenModel;

import java.net.URI;
import java.util.List;

@ApiModel
public class SpiderMutagenDTO {

    @ApiModelProperty(value = "Number of legs")
    private Integer legsNumber;

    @ApiModelProperty(value = "URIs of the linked devices")
    private List<URI> linkedDevices;

    @ApiModelProperty(value = "icon value")
    private String hasIcon;

    public Integer getLegsNumber() {
        return legsNumber;
    }

    public void setLegsNumber(Integer legsNumber) {
        this.legsNumber = legsNumber;
    }

    public List<URI> getLinkedDevices() {
        return linkedDevices;
    }

    public void setLinkedDevices(List<URI> linkedDevices) {
        this.linkedDevices = linkedDevices;
    }

    public String getHasIcon() {
        return hasIcon;
    }

    public void setHasIcon(String hasIcon) {
        this.hasIcon = hasIcon;
    }

    public SpiderMutagenModel toModel() {
        SpiderMutagenModel model = new SpiderMutagenModel();
        model.setHasIcon(getHasIcon());
        model.setLegsNumber(getLegsNumber());
        model.setLinkedDevices(getLinkedDevices());

        return model;
    }
}