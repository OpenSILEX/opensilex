package org.opensilex.migration;

import org.apache.jena.graph.Node;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class BaseGraphMigration extends DatabaseMigrationModuleUpdate {

    @Override
    public String getDescription() {
        return "Rename graph uris";
    }

    @Override
    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) {
        return true;
    }

    @Override
    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {

        List<String> allGraphToChange = Arrays.asList(
                ProfileModel.GRAPH,
                GroupModel.GRAPH,
                VariableModel.GRAPH,
                ProjectModel.GRAPH,
                EventModel.GRAPH,
                DeviceModel.GRAPH,
                FactorModel.GRAPH,
                AccountModel.GRAPH,
                GermplasmModel.GRAPH,
                ScientificObjectModel.GRAPH,
                VariablesGroupModel.GRAPH,
                GermplasmGroupModel.GRAPH,
                AnnotationModel.GRAPH,
                DocumentModel.GRAPH,
                ExperimentModel.GRAPH,
                OrganizationModel.GRAPH
        );

        for (String graph : allGraphToChange) {
            URI oldGraph = URI.create(sparql.getBaseURI() + "set/" + graph);
            URI newGraph = URI.create(sparql.getBaseGraphURI() + "set/" + graph);
            sparql.renameGraph(oldGraph, newGraph);
        }

    }
}
