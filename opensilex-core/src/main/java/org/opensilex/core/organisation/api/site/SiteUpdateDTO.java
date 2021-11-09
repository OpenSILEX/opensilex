package org.opensilex.core.organisation.api.site;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class SiteUpdateDTO extends SiteCreationDTO {
    @Override
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}
