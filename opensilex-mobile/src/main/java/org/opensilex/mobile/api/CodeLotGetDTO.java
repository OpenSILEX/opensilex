//******************************************************************************
//                          CodeLotGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.opensilex.mobile.dal.CodeLotModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Maximilian Hart
 */
public class CodeLotGetDTO {
    protected URI uri;

    private String head;

    private List<URI> availableChildren;

    @NotNull
    @ValidURI
    @ApiModelProperty(value = "URI of the form being updated", required = true) 
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @JsonProperty("available_children")
    public List<URI> getChildren() {
        return availableChildren;
    }

    public void setChildren(List<URI> children) {
        this.availableChildren = children;
    }

    @JsonProperty("head")
    @NotNull
    @ApiModelProperty(value = "topmost code lot", required = true)
    public String getHead(){
        return head;
    }

    public void setHead(String s){
        this.head = s;
    }

    public static CodeLotGetDTO fromModel(CodeLotModel model) {
        CodeLotGetDTO codeLotGetDTO = new CodeLotGetDTO();
        codeLotGetDTO.setUri(model.getUri());
        codeLotGetDTO.setHead(model.getHead());
        codeLotGetDTO.setChildren(model.getAvailableChildren().stream().map(e -> e.getUri()).collect(Collectors.toList()));
        return codeLotGetDTO;
    }

// Not needed as it's get only?
//    public CodeLotModel newModel() {
//        CodeLotModel model = new CodeLotModel();
//        model.setHead(head);
//        model.setAvailableChildren(availableChildren);
//        model.setUri(uri);
//
//        return model;
//
//    }
}
