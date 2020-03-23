/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

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
        sparql.create(instance);
        return instance;
    }
    
    public void create(List<GermplasmModel> instances) throws Exception {
        sparql.create(instances);
    }

    public boolean germplasmLabelExists(String label) throws Exception {
        return sparql.existsByUniquePropertyValue(
                GermplasmModel.class,
                RDFS.label,
                label
        );
    }

    public GermplasmModel create(
            URI uri, 
            String label, 
            URI rdfType, 
            URI fromSpecies, 
            URI fromVariety, 
            URI fromAccession
    ) throws Exception {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setUri(uri);
        germplasm.setLabel(label);
        germplasm.setRdfType(rdfType);
        
        if (fromAccession != null) {
            germplasm.setAccession(fromAccession);
        } else {
            if (fromVariety != null) {
                germplasm.setVariety(fromVariety);
            } else {
                germplasm.setSpecies(fromSpecies);            
            }
        }
      
        sparql.create(germplasm);
        
        return germplasm;
    }  
    
    public GermplasmModel get(URI uri) throws Exception {
        return sparql.getByURI(GermplasmModel.class, uri, null);
    }
    
    public ListWithPagination<GermplasmModel> search(String namePattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

    Expr nameFilter = SPARQLQueryHelper.regexFilter(GermplasmModel.LABEL_VAR, namePattern);

    return sparql.searchWithPagination(
            GermplasmModel.class,
            null,
            (SelectBuilder select) -> {
                if (nameFilter != null) {
                    select.addFilter(nameFilter);
                }
            },
            orderByList,
            page,
            pageSize
    );
}
}
