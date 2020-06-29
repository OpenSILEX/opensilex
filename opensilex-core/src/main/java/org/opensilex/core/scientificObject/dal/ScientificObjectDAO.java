/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.graph.Node;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDAO {

    private final SPARQLService sparql;

    public ScientificObjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public SPARQLPartialTreeListModel<ScientificObjectModel> searchTreeByExperiment(ExperimentDAO xpDAO, URI experimentURI, URI parentURI, int maxChild, int maxDepth, UserModel currentUser) throws Exception {
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.searchPartialResourceTree(
                experimentGraph,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                ScientificObjectModel.PARENT_FIELD,
                parentURI,
                maxChild,
                maxDepth,
                null);
    }

    public List<ScientificObjectModel> searchChildrenByExperiment(ExperimentDAO xpDAO, URI experimentURI, URI parentURI, UserModel currentUser) throws Exception {
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.search(experimentGraph, ScientificObjectModel.class, currentUser.getLanguage(), (select) -> {
            if (parentURI != null) {
                select.addWhere(ScientificObjectModel.URI_FIELD, Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
            } else {
                // TODO
            }

        });
    }
}
