//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.util.List;

/**
 * A taxon: a named group of organisms (scientific name). The taxonomic rank is the rdf:type
 * ({@link Oeso#Species} or {@link Oeso#Genus}).
 * 
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Taxon",
        graph = TaxonModel.GRAPH,
        prefix = "taxon"
)
public class TaxonModel extends SPARQLResourceModel implements ClassURIGenerator<TaxonModel> {
    /**
     * Named graph of the taxa. "germplasm" is the graph of the existing species: keeping it means no data
     * to move. To use a dedicated graph, the migration must move the existing species into it.
     */
    public static final String GRAPH = "germplasm";

    @SPARQLProperty(ontology = RDFS.class, property = "label", required = true)
    private SPARQLLabel label;
    public static final String LABEL_FIELD = "label";

    @SPARQLProperty(ontology = Oeso.class, property = "hasTaxonId")
    private List<String> taxonId;
    public static final String TAXON_ID_FIELD = "taxonId";


    /**
     * Not "required" for the ORM on purpose: legacy species have no kingdom until the
     * SpeciesToTaxonMigration runs, and a required property would hide them from every search.
     * The kingdom is mandatory for creation and update: see TaxonLogic.
     */
    @SPARQLProperty(ontology = Oeso.class, property = "belongsTo")
    private URI kingdom;
    public static final String KINGDOM_FIELD = "kingdom";

    /** Only for a species. */
    @SPARQLProperty(ontology = Oeso.class, property = "hasGenus")
    private URI genus;
    public static final String GENUS_FIELD = "genus";

    public TaxonModel() {
    }

    public TaxonModel(URI uri) {
        setUri(uri);
    }

      /**
     * @return the scientific name. Also used by the existing callers of the germplasm species (DTOs, BrAPI).
     */
    public String getName() {
        return label == null ? null : label.getDefaultValue();
    }

    @Override
    public String[] getInstancePathSegments(TaxonModel instance) {
        String rank = "taxon";
        if (instance.getType() != null && instance.getType().getFragment() != null) {
            rank = instance.getType().getFragment();
        }
        return new String[]{rank, instance.getName()};
    }

    public SPARQLLabel getLabel() {
        return label;
    }

    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }

    public List<String> getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(List<String> taxonId) {
        this.taxonId = taxonId;
    }

    public URI getKingdom() {
        return kingdom;
    }

    public void setKingdom(URI kingdom) {
        this.kingdom = kingdom;
    }

    public URI getGenus() {
        return genus;
    }

    public void setGenus(URI genus) {
        this.genus = genus;
    }
}
