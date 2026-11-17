//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport;

import org.opensilex.OpenSilexModule;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.scope.DataScope;
import org.opensilex.graphimport.graph.scope.EventScope;
import org.opensilex.graphimport.graph.scope.ExperimentScope;
import org.opensilex.graphimport.graph.scope.FacilityScope;
import org.opensilex.graphimport.graph.scope.FactorLevelScope;
import org.opensilex.graphimport.graph.scope.FactorScope;
import org.opensilex.graphimport.graph.scope.GermplasmScope;
import org.opensilex.graphimport.graph.scope.OpenScope;
import org.opensilex.graphimport.graph.scope.OrganizationScope;
import org.opensilex.graphimport.graph.scope.ScientificObjectScope;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.source.HeaderSource;
import org.opensilex.graphimport.interpret.source.InstanceValueSource;
import org.opensilex.graphimport.interpret.source.LabelSource;
import org.opensilex.graphimport.interpret.source.LearnedAliasSource;
import org.opensilex.graphimport.interpret.source.SheetReferenceSource;
import org.opensilex.graphimport.interpret.source.ValueTypeSource;
import org.opensilex.graphimport.validate.CellValueValidator;
import org.opensilex.graphimport.validate.GraphValidator;
import org.opensilex.graphimport.validate.OwlRestrictionGraphValidator;
import org.opensilex.graphimport.write.EventWriter;
import org.opensilex.graphimport.write.ExperimentWriter;
import org.opensilex.graphimport.write.FactorWriter;
import org.opensilex.graphimport.write.ResourceWriter;
import org.opensilex.graphimport.write.ScientificObjectWriter;
import org.opensilex.server.extensions.APIExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * Experimental import assistant.
 * <p>
 * Reads every sheet of a workbook, or of several CSV files, as a graph of the ontology: a row is a
 * resource, a column one of its properties, a cell a value or a link to another resource. Rows that
 * share an identifier across sheets are one resource. The assistant shows what the files would do
 * to the instance, then creates, step by step and when the user asks, what the platform lets it create.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportModule extends OpenSilexModule implements APIExtension, GraphImportExtension {

    public static final String CONFIG_ID = "graph-import";

    @Override
    public Class<?> getConfigClass() {
        return GraphImportConfig.class;
    }

    @Override
    public String getConfigId() {
        return CONFIG_ID;
    }

    @Override
    public List<EvidenceSource> getEvidenceSources() {
        return List.of(new HeaderSource(), new LearnedAliasSource(), new LabelSource(), new ValueTypeSource(),
                new InstanceValueSource(), new SheetReferenceSource());
    }

    @Override
    public List<InstanceScope> getInstanceScopes() {
        List<InstanceScope> scopes = new ArrayList<>(List.of(new ScientificObjectScope(), new ExperimentScope(),
                new GermplasmScope(), new FacilityScope(), new OrganizationScope(), new FactorScope(),
                new FactorLevelScope(), new EventScope(), new DataScope()));
        scopes.addAll(OpenScope.platformDefaults());
        return scopes;
    }

    @Override
    public List<GraphValidator> getGraphValidators() {
        return List.of(new CellValueValidator(), new OwlRestrictionGraphValidator());
    }

    @Override
    public List<ResourceWriter> getResourceWriters() {
        return List.of(new ExperimentWriter(), new FactorWriter(), new ScientificObjectWriter(), new EventWriter());
    }
}
