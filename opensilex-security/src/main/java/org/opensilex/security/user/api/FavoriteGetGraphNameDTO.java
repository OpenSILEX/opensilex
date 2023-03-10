/*******************************************************************************
 *                         FavoriteGetGraphNameDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 08/02/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.security.user.api;

import java.net.URI;

/**
 * Represents a graph/name association. See {@link FavoriteGetDTO} for its usage.
 *
 * @author Valentin Rigolle
 */
public class FavoriteGetGraphNameDTO {
    private URI graph;

    private String name;

    public FavoriteGetGraphNameDTO(URI graph, String name) {
        this.graph = graph;
        this.name = name;
    }

    public URI getGraph() {
        return graph;
    }

    public void setGraph(URI graph) {
        this.graph = graph;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
