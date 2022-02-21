//******************************************************************************
//                          FormCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
/**
 * This class is the Data Transfer Object that is used upon Form creation
 *
 * @author Maximilian Hart
 */
public class FormCreationDTO {

    private List<URI> sectionUris;
    private List<String> children;
    private List<String> parents;
    private URI type;
    private String codeLot;
    private boolean isRoot;
    private String creationDate;
    private String offset;
    private String commitAddress;

    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = new FormModel(this.codeLot, this.commitAddress, this.isRoot);
        model.setChildren(this.children);
        model.setCommitAddress(commitAddress);
        model.setType(this.type);
        model.setParents(this.parents);
        model.setCodeLot(this.codeLot);
        model.setRoot(this.isRoot);
        model.setSectionUris(this.sectionUris);
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getCreationDate(), getOffset());

        if (parsedDateTimeMongo == null) {
            throw new UnableToParseDateException(getCreationDate());
        } else {
            model.setCreationDate(parsedDateTimeMongo.getInstant());
            model.setLastUpdateDate(parsedDateTimeMongo.getInstant());
            model.setOffset(parsedDateTimeMongo.getOffset());
        }
        return model;

    }
    
    @JsonProperty("section_uris")
    public List<URI> getSectionUris() {
        return sectionUris;
    }

    public void setSectionUris(List<URI> sectionUris) {
        this.sectionUris = sectionUris;
    }

    @JsonProperty("form_children_codes")
    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    @JsonProperty("form_parents_codes")
    public List<String> getParents() {
        return parents;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    @JsonProperty("commit_address")
    @NotNull
    @ApiModelProperty(value = "address of the commit", required = true)
    public String getCommitAddress(){
        return commitAddress;
    }

    public void setCommitAddress(String commitAddress){
        this.commitAddress = commitAddress;
    }

    @JsonProperty("type")
    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    @JsonProperty("code_lot")
    @NotNull
    @ApiModelProperty(value = "code lot of the form", required = true)
    public String getCodeLot() {
        return codeLot;
    }

    public void setCodeLot(String codeLot) {
        this.codeLot = codeLot;
    }

    @JsonProperty("is_root")
    @NotNull
    @ApiModelProperty(value = "boolean", required = true)
    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    @JsonProperty("created_date")
    @ApiModelProperty(value = "timestamp", example = "YYYY-MM-DDTHH:MM:SSZ", required = true)
    @Date(DateFormat.YMDTHMSX)
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @ApiModelProperty(value = "to specify if the offset is not in the date and if the timezone is different from the default one")
    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }
    
}
