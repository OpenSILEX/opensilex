//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.graphimport.preview.NodeComparison;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Creates experiments the way the platform's experiment API does: the model filled from the values,
 * the user as publisher, then the experiment DAO. The properties it writes are those of the model, listed
 * below; a resource holding another one is refused rather than created without it.
 *
 * @author Arnaud Charleroy
 */
public class ExperimentWriter implements ResourceWriter {

    public static final String STEP = "experiments";

    static final URI NAME = URI.create(RDFS.label.getURI());
    static final URI DESCRIPTION = URI.create(RDFS.comment.getURI());
    static final URI ALTERNATIVE_NAME = URI.create(SKOS.altLabel.getURI());
    static final URI OBJECTIVE = URI.create(Oeso.hasObjective.getURI());
    static final URI START = URI.create(Oeso.startDate.getURI());
    static final URI END = URI.create(Oeso.endDate.getURI());
    static final URI PUBLIC = URI.create(Oeso.isPublic.getURI());
    static final URI PROJECT = URI.create(Oeso.hasProject.getURI());
    static final URI ORGANIZATION = URI.create(Oeso.usesOrganization.getURI());
    static final URI FACILITY = URI.create(Oeso.usesFacility.getURI());

    private static final Set<URI> WRITTEN = Set.of(NAME, DESCRIPTION, ALTERNATIVE_NAME, OBJECTIVE, START, END, PUBLIC,
            PROJECT, ORGANIZATION, FACILITY);

    @Override
    public String step() {
        return STEP;
    }

    @Override
    public Set<URI> rootClasses() {
        return Set.of(URI.create(Oeso.Experiment.getURI()));
    }

    @Override
    public String credentialId() {
        return ExperimentAPI.CREDENTIAL_EXPERIMENT_MODIFICATION_ID;
    }

    @Override
    public List<String> after() {
        return List.of();
    }

    @Override
    public Optional<ReportMessage> whyNot(NodeComparison node, WriteContext context) {
        NodeValues values = NodeValues.of(node);
        for (URI property : values.properties()) {
            if (!WRITTEN.contains(property)) {
                return Optional.of(CreationMessages.unwritable(property));
            }
        }
        for (URI dated : List.of(START, END)) {
            Optional<String> date = values.literal(dated);
            if (date.isPresent() && parseDate(date.get()).isEmpty()) {
                return Optional.of(CreationMessages.unreadable(dated, date.get()));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<WriteProblem> check(List<NodeComparison> nodes, WriteContext context) {
        List<WriteProblem> problems = new ArrayList<>();
        for (NodeComparison node : nodes) {
            whyNot(node, context).ifPresent(reason -> problems.add(WriteProblem.of(node, reason)));
        }
        return problems;
    }

    @Override
    public List<URI> write(List<NodeComparison> nodes, WriteContext context) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(context.sparql(), context.nosql(), context.fs());
        List<URI> created = new ArrayList<>();
        context.sparql().startTransaction();
        try {
            for (NodeComparison node : nodes) {
                ExperimentModel experiment = modelOf(node);
                experiment.setPublisher(context.user().getUri());
                created.add(dao.create(experiment).getUri());
            }
            context.sparql().commitTransaction();
        } catch (Exception e) {
            context.sparql().rollbackTransaction(e);
            throw e;
        }
        return created;
    }

    /** The experiment model the values of the resource give. */
    static ExperimentModel modelOf(NodeComparison node) {
        NodeValues values = NodeValues.of(node);
        ExperimentModel experiment = new ExperimentModel();
        experiment.setName(values.literal(NAME).orElse(node.node().getName()));
        values.literal(DESCRIPTION).ifPresent(experiment::setDescription);
        values.literal(ALTERNATIVE_NAME).ifPresent(experiment::setAlternativeName);
        values.literal(OBJECTIVE).ifPresent(experiment::setObjective);
        values.literal(START).flatMap(ExperimentWriter::parseDate).ifPresent(experiment::setStartDate);
        values.literal(END).flatMap(ExperimentWriter::parseDate).ifPresent(experiment::setEndDate);
        values.literal(PUBLIC).ifPresent(flag -> experiment.setIsPublic(Boolean.parseBoolean(flag)));
        experiment.setProjects(models(values.targets(PROJECT), uri -> {
            ProjectModel project = new ProjectModel();
            project.setUri(uri);
            return project;
        }));
        experiment.setOrganizations(models(values.targets(ORGANIZATION), uri -> {
            OrganizationModel organization = new OrganizationModel();
            organization.setUri(uri);
            return organization;
        }));
        experiment.setFacilities(models(values.targets(FACILITY), uri -> {
            FacilityModel facility = new FacilityModel();
            facility.setUri(uri);
            return facility;
        }));
        return experiment;
    }

    /** A date as a literal of the date type is written; the day of a date and time. */
    static Optional<LocalDate> parseDate(String lexical) {
        try {
            return Optional.of(LocalDate.parse(lexical.length() > 10 ? lexical.substring(0, 10) : lexical));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static <T> List<T> models(List<URI> uris, Function<URI, T> model) {
        return uris.stream().map(model).toList();
    }
}
