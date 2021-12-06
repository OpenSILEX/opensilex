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

public class FormCreationDTO {

    private List<URI> sectionUris;
    private List<URI> children;
    private List<URI> parents;
    private List<URI> availableChildren;
    private String type;
    private String codeLot;
    private boolean isRoot;
    private String creationDate;
    private String offset;
    private String commitAddress;

    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = new FormModel(this.codeLot, this.commitAddress, this.isRoot);
        model.setAvailableChildren(this.availableChildren);
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

    @JsonProperty("form_children_uris")
    public List<URI> getChildren() {
        return children;
    }

    public void setChildren(List<URI> children) {
        this.children = children;
    }

    @JsonProperty("form_parents_uris")
    public List<URI> getParents() {
        return parents;
    }

    public void setParents(List<URI> parents) {
        this.parents = parents;
    }

    @JsonProperty("empty_children_uris")
    public List<URI> getAvailableChildren() {
        return availableChildren;
    }

    @JsonProperty("commit_address")
    @NotNull
    @ApiModelProperty(value = "address of the commit", required = true)
    public String getCommitAddress(){
        return commitAddress;
    }

    public void setCommitAddress(String s){
        this.commitAddress = s;
    }

    public void setAvailableChildren(List<URI> availableChildren) {
        this.availableChildren = availableChildren;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
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
