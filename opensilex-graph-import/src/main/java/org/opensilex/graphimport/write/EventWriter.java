//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.apache.jena.vocabulary.RDF;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.event.api.EventAPI;
import org.opensilex.core.event.bll.EventLogic;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.ontology.dal.ClassModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Creates events the way the platform's event API does: the type, what they concern, their moments and
 * description on the model, every other value as a relation the platform checks, then the event logic.
 * <p>
 * An event has no name, so the preview cannot find one the instance already holds: an event of the same
 * type, concerning the same resources, with the same moments, counts as already there and is not created
 * again. One date gives an instant event, which the platform holds as its end; two give its start and end.
 *
 * @author Arnaud Charleroy
 */
public class EventWriter implements ResourceWriter {

    public static final String STEP = "events";

    static final URI EVENT = URI.create("http://www.opensilex.org/vocabulary/oeev#Event");
    static final URI CONCERNS = URI.create("http://www.opensilex.org/vocabulary/oeev#concerns");
    static final URI IS_INSTANT = URI.create("http://www.opensilex.org/vocabulary/oeev#isInstant");
    static final URI BEGINNING = URI.create("http://www.w3.org/2006/time#hasBeginning");
    static final URI END = URI.create("http://www.w3.org/2006/time#hasEnd");
    private static final URI TYPE = URI.create(RDF.type.getURI());
    private static final Set<URI> ON_MODEL = Set.of(TYPE, CONCERNS, IS_INSTANT, BEGINNING, END,
            ExperimentWriter.DESCRIPTION);

    @Override
    public String step() {
        return STEP;
    }

    @Override
    public Set<URI> rootClasses() {
        return Set.of(EVENT);
    }

    @Override
    public String credentialId() {
        return EventAPI.CREDENTIAL_EVENT_MODIFICATION_ID;
    }

    @Override
    public List<String> after() {
        return List.of(ExperimentWriter.STEP, ScientificObjectWriter.STEP);
    }

    @Override
    public Optional<ReportMessage> whyNot(NodeComparison node, WriteContext context) {
        NodeValues values = NodeValues.of(node);
        Optional<String> start = values.literal(BEGINNING);
        Optional<String> end = values.literal(END);
        if (start.isEmpty() && end.isEmpty()) {
            return Optional.of(CreationMessages.noMoment());
        }
        for (URI moment : List.of(BEGINNING, END)) {
            Optional<String> value = values.literal(moment);
            if (value.isPresent() && moment(value.get()).isEmpty()) {
                return Optional.of(CreationMessages.unreadable(moment, value.get()));
            }
        }
        return values.targets(CONCERNS).isEmpty() && values.rows(CONCERNS).isEmpty()
                ? Optional.of(CreationMessages.noTarget()) : Optional.empty();
    }

    @Override
    public List<WriteProblem> check(List<NodeComparison> nodes, WriteContext context) throws Exception {
        List<WriteProblem> problems = new ArrayList<>();
        EventLogic<EventModel, EventSearchFilter> logic = logic(context);
        Map<URI, ClassModel> classes = new HashMap<>();
        for (NodeComparison node : nodes) {
            Optional<ReportMessage> reason = whyNot(node, context);
            if (reason.isPresent()) {
                problems.add(WriteProblem.of(node, reason.get()));
                continue;
            }
            try {
                logic.setEventRelations(modelOf(node), relationsOf(node), node.classUri(), classes);
            } catch (Exception e) {
                problems.add(WriteProblem.of(node, CreationMessages.refused(String.valueOf(e.getMessage()))));
            }
        }
        return problems;
    }

    @Override
    public List<URI> write(List<NodeComparison> nodes, WriteContext context) throws Exception {
        EventLogic<EventModel, EventSearchFilter> logic = logic(context);
        Map<URI, ClassModel> classes = new HashMap<>();
        List<EventModel> models = new ArrayList<>();
        for (NodeComparison node : nodes) {
            models.add(logic.setEventRelations(modelOf(node), relationsOf(node), node.classUri(), classes));
        }
        context.sparql().startTransaction();
        try {
            List<URI> created = logic.create(models, false).stream().map(EventModel::getUri).toList();
            context.sparql().commitTransaction();
            return created;
        } catch (Exception e) {
            context.sparql().rollbackTransaction(e);
            throw e;
        }
    }

    /**
     * The events the instance already holds: same type, same resources concerned, same moments. The
     * preview cannot tell, an event having no name.
     */
    @Override
    public Set<NodeComparison> existing(List<NodeComparison> nodes, WriteContext context) throws Exception {
        EventDAO<EventModel, EventSearchFilter> dao = new EventDAO<>(context.sparql(), context.nosql(),
                EventModel.class);
        Set<NodeComparison> existing = new HashSet<>();
        for (NodeComparison node : nodes) {
            if (whyNot(node, context).isPresent()) {
                continue;
            }
            EventModel wanted = modelOf(node);
            EventSearchFilter filter = new EventSearchFilter().setType(wanted.getType()).setTargets(wanted.getTargets());
            filter.setPageSize(0);
            if (dao.search(filter).getList().stream().anyMatch(held -> sameMoments(held, wanted))) {
                existing.add(node);
            }
        }
        return existing;
    }

    private static EventLogic<EventModel, EventSearchFilter> logic(WriteContext context) throws Exception {
        return new EventLogic<>(context.sparql(), context.nosql(), context.user(), EventModel.class);
    }

    private static EventModel modelOf(NodeComparison node) {
        NodeValues values = NodeValues.of(node);
        EventModel event = new EventModel();
        event.setType(node.classUri());
        event.setTargets(values.targets(CONCERNS).stream().map(EventWriter::platformForm).map(URI::create).toList());
        values.literal(ExperimentWriter.DESCRIPTION).ifPresent(event::setDescription);
        Optional<OffsetDateTime> start = values.literal(BEGINNING).flatMap(EventWriter::moment);
        Optional<OffsetDateTime> end = values.literal(END).flatMap(EventWriter::moment);
        if (end.isEmpty()) {
            // One date: an instant event, which the platform holds as its end.
            event.setEnd(instant(start.orElseThrow()));
            event.setIsInstant(true);
        } else {
            start.ifPresent(moment -> event.setStart(instant(moment)));
            event.setEnd(instant(end.get()));
            event.setIsInstant(start.isEmpty());
        }
        return event;
    }

    /** Every value the model does not hold, as the platform's relations. */
    private static List<RDFObjectRelationDTO> relationsOf(NodeComparison node) {
        NodeValues values = NodeValues.of(node);
        List<RDFObjectRelationDTO> relations = new ArrayList<>();
        for (URI property : values.properties()) {
            if (ON_MODEL.contains(property)) {
                continue;
            }
            values.literals(property).forEach(value -> relations.add(new RDFObjectRelationDTO(property, value, false)));
            values.targets(property).forEach(target -> relations.add(new RDFObjectRelationDTO(property,
                    platformForm(target), false)));
        }
        return relations;
    }

    private static boolean sameMoments(EventModel held, EventModel wanted) {
        return sameInstant(held.getStart(), wanted.getStart()) && sameInstant(held.getEnd(), wanted.getEnd());
    }

    private static boolean sameInstant(InstantModel held, InstantModel wanted) {
        if (held == null || wanted == null) {
            return held == wanted || (held != null ? held.getDateTimeStamp() == null : wanted.getDateTimeStamp() == null);
        }
        return Objects.equals(held.getDateTimeStamp() == null ? null : held.getDateTimeStamp().toInstant(),
                wanted.getDateTimeStamp() == null ? null : wanted.getDateTimeStamp().toInstant());
    }

    private static InstantModel instant(OffsetDateTime moment) {
        InstantModel instant = new InstantModel();
        instant.setDateTimeStamp(moment);
        return instant;
    }

    /** A moment as the reading of the file gives it: a date and time with its offset. */
    static Optional<OffsetDateTime> moment(String lexical) {
        try {
            return Optional.of(OffsetDateTime.parse(lexical));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /** A URI as the platform writes it in its models, with its prefix. */
    private static String platformForm(URI uri) {
        return SPARQLDeserializers.getShortURI(uri.toString());
    }
}
