/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author boizetal
 */
public class GermplasmDAO {
    
    protected final SPARQLService sparql;
    
    public GermplasmDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public GermplasmModel create(GermplasmModel instance) throws Exception {
        checkURIs(instance);
        sparql.create(instance);
        return instance;
    }
    
        protected void checkURIs(GermplasmModel model) throws SPARQLException, IllegalArgumentException, URISyntaxException {

        // #TODO use a method to test in one query if multiple URI(s) exists and are of a given type, or use SHACL validation

        checkURIs(model.getGenus(), (Oeso.Genus));
        checkURIs(model.getSpecies(), (Oeso.Species));
        checkURIs(model.getVariety(), (Oeso.Variety));
        checkURIs(model.getAccession(), (Oeso.Accession));

    }
}
