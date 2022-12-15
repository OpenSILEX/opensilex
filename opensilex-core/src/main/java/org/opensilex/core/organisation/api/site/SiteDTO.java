package org.opensilex.core.organisation.api.site;

import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * @author Valentin RIGOLLE
 */
public class SiteDTO extends NamedResourceDTO<SiteModel> {
    @Override
    public SiteModel newModelInstance() {
        return new SiteModel();
    }
}
