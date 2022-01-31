//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.model;

import java.net.URI;

public interface SPARQLModel {

    URI getUri();

    void setUri(URI uri);

    URI getType();

    void setType(URI type);

    SPARQLLabel getTypeLabel();

    void setTypeLabel(SPARQLLabel typeLabel);

}
