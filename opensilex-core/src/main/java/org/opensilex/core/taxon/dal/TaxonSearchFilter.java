//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.dal;

import org.opensilex.sparql.service.SearchFilter;

import java.net.URI;

/**
 * Search filter for taxa. Rank filtering uses {@link SearchFilter#getRdfTypes()} (Species, Genus).
 */
public class TaxonSearchFilter extends SearchFilter {

    private String name;
    private URI kingdom;
    private String taxonId;

    public String getName() {
        return name;
    }

    public TaxonSearchFilter setName(String name) {
        this.name = name;
        return this;
    }

    public URI getKingdom() {
        return kingdom;
    }

    public TaxonSearchFilter setKingdom(URI kingdom) {
        this.kingdom = kingdom;
        return this;
    }

    public String getTaxonId() {
        return taxonId;
    }

    public TaxonSearchFilter setTaxonId(String taxonId) {
        this.taxonId = taxonId;
        return this;
    }

}
