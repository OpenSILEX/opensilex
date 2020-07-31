/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.dal;

import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vmigot
 */
public class VueOwlExtensionDAO {

    private final SPARQLService sparql;

    public VueOwlExtensionDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public void createExtendedClass(ClassModel instance, VueClassExtensionModel instanceExtension) throws Exception {
        try {
            sparql.startTransaction();
            sparql.create(instance);
            sparql.create(instanceExtension);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }
}
