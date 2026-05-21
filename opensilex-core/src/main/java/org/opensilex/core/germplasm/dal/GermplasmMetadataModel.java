package org.opensilex.core.germplasm.dal;

import org.opensilex.nosql.mongodb.metadata.MetaDataModel;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class GermplasmMetadataModel extends MetaDataModel {
    private Boolean isPublic;
    private List<URI> groups;

    public GermplasmMetadataModel() {
        super();
    }

    public GermplasmMetadataModel(Map<String, String> attributes) {
        super(attributes);
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }
}
