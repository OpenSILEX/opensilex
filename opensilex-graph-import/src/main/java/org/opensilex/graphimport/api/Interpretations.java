//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api;

import org.opensilex.OpenSilex;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.GraphImportExtension;
import org.opensilex.graphimport.api.dto.InterpretationDTO;
import org.opensilex.graphimport.api.dto.PreviewNodeDTO;
import org.opensilex.graphimport.api.dto.ValidationDTO;
import org.opensilex.graphimport.graph.DateColumn;
import org.opensilex.graphimport.graph.GraphBuilder;
import org.opensilex.graphimport.graph.GraphLinks;
import org.opensilex.graphimport.graph.GuardedInstanceIndex;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.ClassCatalogReader;
import org.opensilex.graphimport.interpret.ClassViews;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.Interpreter;
import org.opensilex.graphimport.interpret.SheetFits;
import org.opensilex.graphimport.interpret.alias.LearnedAliasStore;
import org.opensilex.graphimport.measure.DataDraft;
import org.opensilex.graphimport.measure.MeasureReader;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.ClassViewReader;
import org.opensilex.graphimport.ontology.HeaderResolver;
import org.opensilex.graphimport.ontology.ModuleOntology;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.preview.ImportPreview;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.graphimport.preview.NodeState;
import org.opensilex.graphimport.preview.PreviewBuilder;
import org.opensilex.graphimport.preview.PreviewMemo;
import org.opensilex.graphimport.preview.ValueComparison;
import org.opensilex.graphimport.session.GraphImportSession;
import org.opensilex.graphimport.validate.GraphProblem;
import org.opensilex.graphimport.validate.GraphValidation;
import org.opensilex.graphimport.validate.GraphValidator;
import org.opensilex.graphimport.validate.ValidationReport;
import org.opensilex.graphimport.workbook.WorkbookAdditions;
import org.opensilex.graphimport.write.CreationPlan;
import org.opensilex.graphimport.write.DataPlan;
import org.opensilex.graphimport.write.DataWriter;
import org.opensilex.graphimport.write.ResourceWriter;
import org.opensilex.graphimport.write.WriteContext;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interprets an import for one request, and checks the graph it gives: the sources, classes and
 * validators the modules bring, the classes a sheet may hold, the views of those classes, each read
 * once and dropped with the request.
 *
 * @author Arnaud Charleroy
 */
class Interpretations {

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;
    private final List<EvidenceSource> sources = new ArrayList<>();
    private final List<InstanceScope> scopes = new ArrayList<>();
    private final List<GraphValidator> validators = new ArrayList<>();
    private final List<ResourceWriter> writers = new ArrayList<>();
    private final ClassViews views;
    private ClassCatalog catalog;

    Interpretations(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, OpenSilex opensilex,
                    AccountModel user) throws Exception {
        ModuleOntology.ensureLoaded(sparql);
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
        for (GraphImportExtension extension : opensilex.getModulesImplementingInterface(GraphImportExtension.class)) {
            sources.addAll(extension.getEvidenceSources());
            scopes.addAll(extension.getInstanceScopes());
            validators.addAll(extension.getGraphValidators());
            writers.addAll(extension.getResourceWriters());
        }
        this.views = ClassViews.of(new ClassViewReader(sparql));
    }

    /** The classes a sheet may hold. */
    ClassCatalog catalog() throws Exception {
        if (catalog == null) {
            catalog = new ClassCatalogReader(sparql).read(scopes.stream().map(InstanceScope::rootClass).toList());
        }
        return catalog;
    }

    ClassViews views() {
        return views;
    }

    /**
     * Interprets the import with what the user decided and the experiment they chose. An experiment
     * the user may no longer read is dropped from the import first.
     */
    InterpretationDTO interpret(GraphImportSession session) throws Exception {
        Interpreted interpreted = interpretModel(session);
        return InterpretationDTO.fromModel(session.getId(), session.getExperiment(), interpreted.interpretation(),
                catalog(), interpreted.services().language(), SheetFits.of(interpreted.interpretation(), views),
                session.getReadWorkbook()).withProvenance(session.getProvenance());
    }

    /** Builds the graph of the import as interpreted, and checks it with every module's validators. */
    ValidationDTO validate(GraphImportSession session) throws Exception {
        Interpreted interpreted = interpretModel(session);
        WorkbookAdditions.ReadWorkbook read = session.getReadWorkbook();
        ImportGraph graph = new GraphBuilder(interpreted.services())
                .build(read.workbook(), interpreted.interpretation(), read.decisions());
        List<GraphProblem> problems = new ArrayList<>(GraphValidation.problems(graph, validators, interpreted.services()));
        MeasureReader.Result data = new MeasureReader(interpreted.services())
                .read(read.workbook(), interpreted.interpretation(), read.decisions(), graph, draft -> { });
        problems.addAll(data.problems());
        List<DateColumn> dateColumns = new ArrayList<>(graph.dateColumns());
        dateColumns.addAll(data.dateColumns());
        ImportGraph checked = new ImportGraph(graph.nodes(), graph.notes(), dateColumns);
        ValidationReport report = GraphValidation.report(checked, problems);
        return ValidationDTO.fromModel(session.getId(), report, checked,
                GraphLinks.of(graph, interpreted.interpretation()), catalog(), interpreted.services().language())
                .withData(data.sheets());
    }

    /**
     * What the import would do to the instance. The preview last worked out is given back while it holds
     * for the import as it stands, unless asked afresh, so that turning the pages of the grid does not
     * read the instance again; asking afresh reads what the instance holds now.
     */
    ImportPreview preview(GraphImportSession session, boolean afresh) throws Exception {
        PreviewMemo memo = session.getPreviewMemo();
        WorkbookAdditions.ReadWorkbook read = session.getReadWorkbook();
        if (!afresh && memo != null && memo.holdsFor(read.workbook(), read.decisions(), session.getExperiment())) {
            return memo.preview();
        }
        Interpreted interpreted = interpretModel(session);
        ImportGraph graph = new GraphBuilder(interpreted.services())
                .build(read.workbook(), interpreted.interpretation(), read.decisions());
        List<GraphProblem> problems = GraphValidation.problems(graph, validators, interpreted.services());
        ImportPreview nodes = new PreviewBuilder(interpreted.services())
                .build(interpreted.interpretation(), graph, problems);
        ImportPreview preview = nodes.withData(new MeasureReader(interpreted.services())
                .read(read.workbook(), interpreted.interpretation(), read.decisions(), graph, held(nodes), draft -> { })
                .sheets());
        session.setPreviewMemo(new PreviewMemo(read.workbook(), read.decisions(), session.getExperiment(), preview));
        return preview;
    }

    /**
     * What the creation step would do, from the preview: read afresh after a step wrote, so that what it
     * created reads as done.
     */
    Creation creation(GraphImportSession session, boolean afresh) throws Exception {
        ImportPreview preview = preview(session, afresh);
        WriteContext context = WriteContext.of(sparql, nosql, fs, user, session.getExperiment(), catalog(),
                preview.getNodes());
        CreationPlan plan = CreationPlan.of(preview.getNodes(), writers, context);
        Map<String, Set<NodeComparison>> existing = new HashMap<>();
        for (CreationPlan.Step step : plan.steps()) {
            if (!step.ready().isEmpty()) {
                existing.put(step.name(), step.writer().existing(step.ready(), context));
            }
        }
        DataWriter data = new DataWriter(context, session.getProvenance());
        return new Creation(plan.withExisting(existing), context, data, dataPlan(session, preview, data));
    }

    /**
     * The data of the sheets of data, read again from the files: they are not kept, a sheet may give tens
     * of thousands. A datum measured on a row of the file is ready once the preview finds that row in the
     * instance.
     */
    private DataPlan dataPlan(GraphImportSession session, ImportPreview preview, DataWriter writer) throws Exception {
        if (preview.getData().isEmpty()) {
            return writer.plan(List.of(), List.of());
        }
        Interpreted interpreted = interpretModel(session);
        WorkbookAdditions.ReadWorkbook read = session.getReadWorkbook();
        ImportGraph graph = new GraphBuilder(interpreted.services())
                .build(read.workbook(), interpreted.interpretation(), read.decisions());
        List<DataDraft> drafts = new ArrayList<>();
        MeasureReader.Result result = new MeasureReader(interpreted.services()).read(read.workbook(),
                interpreted.interpretation(), read.decisions(), graph, held(preview), drafts::add);
        return writer.plan(drafts, result.sheets());
    }

    /** For the key of a row of the files, the resource of the instance the preview found it to be. */
    private static Function<String, Optional<URI>> held(ImportPreview preview) {
        Map<String, URI> held = new HashMap<>();
        preview.getNodes().stream()
                .filter(node -> node.state() != NodeState.CREATED && node.existing() != null)
                .forEach(node -> held.put(node.node().getKey(), node.existing()));
        return key -> Optional.ofNullable(held.get(key));
    }

    /** The plan of the creation, and what its writers work with. */
    record Creation(CreationPlan plan, WriteContext context, DataWriter dataWriter, DataPlan data) {

        /** The steps the user may run, by the platform's credentials. */
        Set<String> allowed() throws Exception {
            Set<String> allowed = new HashSet<>();
            for (CreationPlan.Step step : plan.steps()) {
                if (context.mayWrite(step.writer().credentialId())) {
                    allowed.add(step.name());
                }
            }
            if (context.mayWrite(DataWriter.credentialId())) {
                allowed.add(DataWriter.STEP);
            }
            return allowed;
        }
    }

    /** A resource of the preview, its classes and properties named in the user's language. */
    PreviewNodeDTO describe(NodeComparison node) throws Exception {
        String language = user.getLanguage();
        ClassCatalog classes = catalog();
        String classLabels = node.node().getClasses().stream()
                .map(classUri -> classes.labelOf(classUri, language))
                .collect(Collectors.joining(", "));
        ClassView view = node.classUri() == null ? null : views.get(node.classUri());
        Map<URI, String> propertyLabels = new HashMap<>();
        for (ValueComparison value : node.values()) {
            PropertyView property = view == null ? null : view.getProperty(value.property());
            String label = property == null ? null : property.labels().in(language);
            propertyLabels.put(value.property(), label != null ? label : ClassCatalog.localName(value.property()));
        }
        return PreviewNodeDTO.fromModel(node, classLabels, propertyLabels);
    }

    /** An interpretation, with the lookups it was made with, which the graph goes on using. */
    private record Interpreted(Interpretation interpretation, InterpretationServices services) {
    }

    private Interpreted interpretModel(GraphImportSession session) throws Exception {
        LookupContext context;
        try {
            context = LookupContext.of(sparql, nosql, fs, user, session.getExperiment());
        } catch (ForbiddenURIAccessException | SPARQLInvalidUriListException e) {
            // The experiment was deleted, or its access withdrawn, since it was chosen: the import goes on
            // without it, rather than every screen of it failing until someone edits the store.
            session.setExperiment(null);
            context = LookupContext.of(sparql, nosql, fs, user, null);
        }
        InterpretationServices services = new InterpretationServices(catalog(), views,
                new GuardedInstanceIndex(context, scopes), new LearnedAliasStore(nosql),
                HeaderResolver.ofPlatformPrefixes(), user.getUri(), user.getLanguage());
        WorkbookAdditions.ReadWorkbook read = session.getReadWorkbook();
        Interpretation interpretation = new Interpreter(services, sources)
                .interpret(read.workbook(), read.decisions(), session.getExperiment());
        return new Interpreted(interpretation, services);
    }
}
