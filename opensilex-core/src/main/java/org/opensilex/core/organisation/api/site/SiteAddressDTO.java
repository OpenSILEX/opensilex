package org.opensilex.core.organisation.api.site;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opensilex.core.address.api.AddressDTO;
import org.opensilex.core.organisation.dal.SiteAddressModel;
import org.opensilex.core.organisation.dal.SiteModel;
import org.opensilex.sparql.response.NamedResourceDTO;

public class SiteAddressDTO extends AddressDTO<SiteAddressModel> {
    protected NamedResourceDTO<SiteModel> site;

    @JsonIgnore
    public NamedResourceDTO<SiteModel> getSite() {
        return site;
    }

    public void setSite(NamedResourceDTO<SiteModel> site) {
        this.site = site;
    }

    @Override
    public SiteAddressModel newModelInstance() {
        return new SiteAddressModel();
    }

    @Override
    public void fromModel(SiteAddressModel model) {
        super.fromModel(model);

        SiteModel siteModel = model.getSite();
        if (siteModel != null) {
            setSite(NamedResourceDTO.getDTOFromModel(siteModel));
        }
    }

    @Override
    public void toModel(SiteAddressModel model) {
        super.toModel(model);

        if (getSite() != null) {
            SiteModel siteModel = new SiteModel();
            siteModel.setUri(getSite().getUri());
            model.setSite(siteModel);
        }
    }
}
