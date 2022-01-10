package org.opensilex.core.organisation.api.site;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * Update DTO for a site. If the address is specified, the geometry will be deduced by a geocoding service.
 *
 * @author Valentin RIGOLLE
 */
public class SiteUpdateDTO extends SiteCreationDTO {
    @Override
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}
