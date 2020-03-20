//******************************************************************************
//                          SpeciesDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.species.dal;

import org.opensilex.sparql.service.SPARQLService;

import java.util.List;


/**
 * @author Renaud COLIN
 */
public class SpeciesDAO {

    protected final SPARQLService sparql;

    public SpeciesDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    /**
     *
     * @param lang the {@link SpeciesModel} {@link org.apache.jena.vocabulary.RDFS#label} lang
     * @return the list of all {@link SpeciesModel} in the given lang
     */
    public List<SpeciesModel> getAll(String lang) throws Exception {
//        if(StringUtils.isEmpty(lang)){
//            Locale langLocale = new Locale.Builder().setLanguageTag(OpenSilex.DEFAULT_LANGUAGE).build();
//            lang = langLocale.getLanguage();
//        }
        return sparql.search(SpeciesModel.class,lang);
    }


}
