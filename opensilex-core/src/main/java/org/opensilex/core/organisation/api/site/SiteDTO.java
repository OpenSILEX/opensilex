package org.opensilex.core.organisation.api.site;

import org.opensilex.core.organisation.dal.SiteModel;
import org.opensilex.sparql.response.NamedResourceDTO;

public class SiteDTO extends NamedResourceDTO<SiteModel> {
    @Override
    public SiteModel newModelInstance() {
        return new SiteModel();
    }
}
