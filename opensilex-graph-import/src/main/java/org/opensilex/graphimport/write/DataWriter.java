//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.bll.DataValidation;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.graphimport.measure.DataDraft;
import org.opensilex.graphimport.measure.DataSheetSummary;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates the data of the sheets of data through the platform's data logic, which checks them as the
 * data API does: the variable, the target in the experiment, the provenance, the type of the value.
 * <p>
 * Data are written by batches of at most {@link #BATCH}, each all or none. A datum the instance already
 * holds, same provenance, variable, target and date, is not created again: a step stopped after some
 * batches is resumed where it stopped.
 *
 * @author Arnaud Charleroy
 */
public class DataWriter {

    public static final String STEP = "data";
    /** The most data written at once, as the platform's data API takes them. */
    public static final int BATCH = DataAPI.SIZE_MAX;

    private final WriteContext context;
    private final URI provenance;

    /**
     * @param provenance the provenance chosen for the import; null when none is
     */
    public DataWriter(WriteContext context, URI provenance) {
        this.context = context;
        this.provenance = provenance;
    }

    public static String credentialId() {
        return DataAPI.CREDENTIAL_DATA_MODIFICATION_ID;
    }

    public static List<String> after() {
        return List.of(ScientificObjectWriter.STEP);
    }

    /** What the step would do with the data read from the files. */
    public DataPlan plan(List<DataDraft> drafts, List<DataSheetSummary> sheets) throws Exception {
        int notAvailable = sheets.stream().mapToInt(DataSheetSummary::notAvailable).sum();
        int refused = sheets.stream().mapToInt(DataSheetSummary::refused).sum();
        int waiting = (int) drafts.stream().filter(DataDraft::waits).count();
        List<DataDraft> creatable = drafts.stream().filter(draft -> !draft.waits()).toList();
        if (provenance == null && !creatable.isEmpty()) {
            return new DataPlan(CreationPlan.Status.BLOCKED, List.of(), creatable.size(),
                    CreationMessages.noProvenance(), waiting, 0, notAvailable, refused, sheets);
        }
        Set<String> held = held(creatable);
        List<DataDraft> ready = new ArrayList<>();
        for (DataDraft draft : creatable) {
            if (!held.contains(identity(draft))) {
                ready.add(draft);
            }
        }
        int done = creatable.size() - ready.size();
        CreationPlan.Status status = !ready.isEmpty() ? CreationPlan.Status.READY
                : waiting > 0 ? CreationPlan.Status.BLOCKED
                : done > 0 ? CreationPlan.Status.DONE
                : CreationPlan.Status.EMPTY;
        return new DataPlan(status, ready, 0, null, waiting, done, notAvailable, refused, sheets);
    }

    /**
     * Checks the data the way the platform would before writing them, batch by batch, without writing.
     *
     * @return what would stop the step; empty when it would go through
     */
    public List<WriteProblem> check(List<DataDraft> ready) {
        List<WriteProblem> problems = new ArrayList<>();
        List<List<DataDraft>> batches = batches(ready);
        for (int batch = 0; batch < batches.size(); batch++) {
            try {
                new DataValidation(models(batches.get(batch)), context.sparql(), context.nosql(), context.user(),
                        context.fs()).validate();
            } catch (Exception e) {
                problems.add(WriteProblem.ofStep(CreationMessages.batchRefused(batch + 1, batches.size(),
                        String.valueOf(e.getMessage()))));
            }
        }
        return problems;
    }

    /**
     * What a run of the step wrote, and what stopped it when something did.
     *
     * @param refusal what stopped it; null when nothing did
     */
    public record Outcome(int created, WriteProblem refusal) {

        public Optional<WriteProblem> problem() {
            return Optional.ofNullable(refusal);
        }
    }

    /**
     * Writes the data batch by batch. A refused batch stops the step; the batches before it stay written,
     * and are read as already there when the step is run again.
     */
    public Outcome write(List<DataDraft> ready) throws Exception {
        DataLogic logic = new DataLogic(context.sparql(), context.nosql(), context.fs(), context.user());
        int created = 0;
        List<List<DataDraft>> batches = batches(ready);
        for (int batch = 0; batch < batches.size(); batch++) {
            // The platform's checks change the models they read: each batch is built afresh for writing.
            try {
                created += logic.createMany(models(batches.get(batch))).size();
            } catch (Exception e) {
                return new Outcome(created, WriteProblem.ofStep(CreationMessages.batchRefused(batch + 1,
                        batches.size(), String.valueOf(e.getMessage()))));
            }
        }
        return new Outcome(created, null);
    }

    /**
     * The data among these that the instance already holds, by their identity: those of the import's
     * provenance, of the same variables, between the first and the last date, as the user may see them.
     */
    private Set<String> held(List<DataDraft> drafts) throws Exception {
        if (drafts.isEmpty()) {
            return Set.of();
        }
        Set<URI> variables = new HashSet<>();
        Instant first = null;
        Instant last = null;
        for (DataDraft draft : drafts) {
            variables.add(draft.variable());
            Instant date = date(draft).getInstant();
            first = first == null || date.isBefore(first) ? date : first;
            last = last == null || date.isAfter(last) ? date : last;
        }
        // The platform's search takes data before the end date only.
        List<DataModel> stored = new DataDAO(context.nosql(), context.sparql(), context.fs()).search(context.user(),
                null, null, List.copyOf(variables), List.of(provenance), null, first, last.plusMillis(1), null, null,
                null, null, null);
        Set<String> held = new HashSet<>();
        stored.forEach(datum -> held.add(identity(datum.getTarget(), datum.getVariable(), datum.getDate())));
        return held;
    }

    private List<DataModel> models(List<DataDraft> drafts) throws Exception {
        List<DataModel> models = new ArrayList<>();
        for (DataDraft draft : drafts) {
            DataModel model = new DataModel();
            // The platform's checks find the variable and the provenance by their URIs written as its data
            // API receives them.
            model.setTarget(draft.target() == null ? null : platformForm(draft.target()));
            model.setVariable(platformForm(draft.variable()));
            DataProvenanceModel dataProvenance = new DataProvenanceModel();
            dataProvenance.setUri(platformForm(provenance));
            if (context.experiment() != null) {
                dataProvenance.setExperiments(List.of(platformForm(context.experiment())));
            }
            model.setProvenance(dataProvenance);
            ParsedDateTimeMongo date = date(draft);
            model.setDate(date.getInstant());
            model.setOffset(date.getOffset());
            model.setIsDateTime(date.getIsDateTime());
            model.setValue(draft.value());
            models.add(model);
        }
        return models;
    }

    /** The date of a datum as the platform stores it; the reading of the files gave it in a form it reads. */
    private static ParsedDateTimeMongo date(DataDraft draft) throws Exception {
        ParsedDateTimeMongo date = DataValidateUtils.setDataDateInfo(draft.date(), null);
        if (date == null) {
            throw new IllegalStateException("The platform cannot read the date " + draft.date());
        }
        return date;
    }

    private static String identity(DataDraft draft) throws Exception {
        return identity(draft.target(), draft.variable(), date(draft).getInstant());
    }

    /** What makes a datum the same as another, whatever way its URIs are written. */
    private static String identity(URI target, URI variable, Instant date) {
        return (target == null ? "" : SPARQLDeserializers.getExpandedURI(target)) + "\u0000"
                + SPARQLDeserializers.getExpandedURI(variable) + "\u0000" + date.toEpochMilli();
    }

    /** A URI written as the platform's JSON reader gives it, prefixed or not as the platform is set. */
    private static URI platformForm(URI uri) {
        return URIDeserializer.formatURI(uri);
    }

    private static List<List<DataDraft>> batches(List<DataDraft> drafts) {
        List<List<DataDraft>> batches = new ArrayList<>();
        for (int start = 0; start < drafts.size(); start += BATCH) {
            batches.add(drafts.subList(start, Math.min(drafts.size(), start + BATCH)));
        }
        return batches;
    }
}
