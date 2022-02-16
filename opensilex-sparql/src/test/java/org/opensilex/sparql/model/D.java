/*******************************************************************************
 *                         D.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 15/02/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.sparql.model;

import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 * @author Valentin RIGOLLE
 */
@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "D"
)
public class D extends SPARQLResourceModel {
    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "requiredLabel",
            required = true
    )
    private SPARQLLabel requiredLabel;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "optionalLabel"
    )
    private SPARQLLabel optionalLabel;

    public SPARQLLabel getRequiredLabel() {
        return requiredLabel;
    }

    public void setRequiredLabel(SPARQLLabel requiredLabel) {
        this.requiredLabel = requiredLabel;
    }

    public SPARQLLabel getOptionalLabel() {
        return optionalLabel;
    }

    public void setOptionalLabel(SPARQLLabel optionalLabel) {
        this.optionalLabel = optionalLabel;
    }
}
