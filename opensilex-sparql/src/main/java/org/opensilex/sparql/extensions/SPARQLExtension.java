/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.extensions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.OpenSilexExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public interface SPARQLExtension extends OpenSilexExtension {

    public final static Logger LOGGER = LoggerFactory.getLogger(SPARQLExtension.class);

    public default List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        return new ArrayList<>();
    }

    public default void installOntologies(SPARQLService sparql, boolean reset) throws Exception {
        for (OntologyFileDefinition ontologyDef : getOntologiesFiles()) {
            if (reset) {
                sparql.clearGraph(ontologyDef.getGraphURI());
            }
            InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), ontologyDef.getFilePath()));
            sparql.loadOntology(ontologyDef.getGraphURI(), ontologyStream, ontologyDef.getFileType());
            ontologyStream.close();
        }
    }

    public default void checkOntologies(SPARQLService sparql) throws Exception {
        for (OntologyFileDefinition ontologyDef : getOntologiesFiles()) {
            List<SPARQLStatement> results = sparql.getGraphStatement(ontologyDef.getGraphURI());

            if (results.size() == 0) {
                String errorMsg = ontologyDef.getGraphURI().toString() + " is missing data into your triple store, did you execute `opensilex system setup` command ?";
                LOGGER.warn("/!\\ " + errorMsg);
                throw new Exception(errorMsg);
            }
        }
    }

    public default void inMemoryInitialization() throws Exception {
    }

}
