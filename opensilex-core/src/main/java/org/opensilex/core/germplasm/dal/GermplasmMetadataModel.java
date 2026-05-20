package org.opensilex.core.germplasm.dal;

import org.opensilex.nosql.mongodb.metadata.MetaDataModel;

import java.util.Map;

public class GermplasmMetadataModel extends MetaDataModel {
    public GermplasmMetadataModel() {
        super();
    }

    public GermplasmMetadataModel(Map<String, String> attributes) {
        super(attributes);
    }
}
