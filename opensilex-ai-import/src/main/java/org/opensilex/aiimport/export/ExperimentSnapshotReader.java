//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.opensilex.aiimport.export.ExperimentSnapshot.ExportedObject;
import org.opensilex.aiimport.export.ExperimentSnapshot.Facility;
import org.opensilex.aiimport.export.ExperimentSnapshot.Level;
import org.opensilex.aiimport.export.ExperimentSnapshot.ObjectType;
import org.opensilex.aiimport.export.ExperimentSnapshot.Observation;
import org.opensilex.aiimport.export.ExperimentSnapshot.Variable;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;
import org.opensilex.core.utils.StringUriMap;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Reads an experiment from the instance, as the user may see it, into an {@link ExperimentSnapshot}.
 * <p>
 * The only part of the export that knows the platform: every read goes through the platform's own
 * DAO or logic — the same search the scientific-object export runs, the same data search the data
 * export runs — so the rights and the rules are the platform's. The experiment is read through
 * {@link ExperimentDAO#get}, which refuses an experiment the user cannot see.
 * <p>
 * URIs are compared in their expanded form: the platform hands some out prefixed and some not, and
 * a data point would otherwise fail to find the object it was measured on.
 *
 * @author Arnaud Charleroy
 */
public class ExperimentSnapshotReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentSnapshotReader.class);

    /**
     * Beyond this many values, a workbook is no longer the right shape: the platform's own data
     * export is. Refused before anything is read, rather than failing on memory half-way.
     */
    public static final int MAX_OBSERVATIONS = 500_000;

    private static final int PAGE_SIZE = 5_000;

    /**
     * The properties an object sheet gives a column of its own, or that only mean something inside
     * the platform: never repeated among the other properties.
     */
    private static final Set<String> OBJECT_PROPERTIES_HANDLED = Set.of(
            RDF.type.getURI(), RDFS.label.getURI(), RDFS.comment.getURI(), Oeso.isPartOf.getURI(),
            Oeso.hasFactorLevel.getURI(), Oeso.hasGermplasm.getURI(), Oeso.hasCreationDate.getURI(),
            Oeso.hasDestructionDate.getURI(), Oeso.isHosted.getURI(), Oeso.hasGeometry.getURI(),
            Oeso.participatesIn.getURI());

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;

    public ExperimentSnapshotReader(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                                    AccountModel user) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
    }

    /**
     * @throws TooManyObservationsException when the experiment holds more values than a workbook
     *                                      should carry
     */
    public ExperimentSnapshot read(URI experimentUri) throws Exception {
        ExperimentModel experiment = new ExperimentDAO(sparql, nosql, fs).get(experimentUri, user);
        URI uri = expanded(experiment.getUri());

        ExperimentSnapshot snapshot = new ExperimentSnapshot()
                .setUri(uri)
                .setName(experiment.getName())
                .setObjective(experiment.getObjective())
                .setDescription(experiment.getDescription())
                .setStartDate(experiment.getStartDate())
                .setEndDate(experiment.getEndDate());

        Labels labels = new Labels();
        readProjects(experiment, snapshot, labels);
        readOrganizations(experiment, snapshot, labels);
        readContacts(experiment, snapshot);
        Map<URI, String> levelNames = readLevels(uri, snapshot);
        readFacilities(experiment, snapshot, labels);
        readObjects(uri, levelNames, snapshot, labels);
        readObservations(uri, snapshot);
        readVariables(snapshot);
        return snapshot;
    }

    //#region the experiment

    private void readProjects(ExperimentModel experiment, ExperimentSnapshot snapshot, Labels labels)
            throws Exception {
        snapshot.getProjects().addAll(labels.namesOf(urisOf(experiment.getProjects())));
    }

    /**
     * An organisation with a parent is a unit of that parent: STAR names the institution and the
     * unit apart, and the hierarchy says which is which.
     */
    private void readOrganizations(ExperimentModel experiment, ExperimentSnapshot snapshot, Labels labels)
            throws Exception {
        List<URI> uris = urisOf(experiment.getOrganizations());
        if (uris.isEmpty()) {
            return;
        }
        Set<String> institutions = new LinkedHashSet<>();
        Set<String> units = new LinkedHashSet<>();
        for (OrganizationModel organization : new OrganizationDAO(sparql).getByURIs(uris, user.getLanguage())) {
            List<URI> parents = urisOf(organization.getParents());
            if (parents.isEmpty()) {
                institutions.add(organization.getName());
            } else {
                units.add(organization.getName());
                institutions.addAll(labels.namesOf(parents));
            }
        }
        snapshot.getOrganizations().addAll(institutions);
        snapshot.getSuborganizations().addAll(units);
    }

    private void readContacts(ExperimentModel experiment, ExperimentSnapshot snapshot) throws Exception {
        List<URI> persons = new ArrayList<>(urisOf(experiment.getScientificSupervisors()));
        urisOf(experiment.getTechnicalSupervisors()).stream()
                .filter(person -> !persons.contains(person))
                .forEach(persons::add);
        if (persons.isEmpty()) {
            return;
        }
        for (PersonModel person : new PersonDAO(sparql).getList(persons)) {
            if (person.getEmail() != null) {
                snapshot.getEmails().add(person.getEmail().getAddress());
            }
        }
    }

    /**
     * @return the name of every level, by its URI, for the objects that carry them
     */
    private Map<URI, String> readLevels(URI experiment, ExperimentSnapshot snapshot) throws Exception {
        Map<URI, String> names = new HashMap<>();
        for (FactorModel factor : new FactorDAO(sparql).getByExperiment(experiment, user.getLanguage())) {
            if (factor.getFactorLevels() == null) {
                continue;
            }
            for (FactorLevelModel level : factor.getFactorLevels()) {
                snapshot.getLevels().add(new Level(factor.getName(), level.getName(), level.getDescription()));
                names.put(expanded(level.getUri()), level.getName());
            }
        }
        return names;
    }

    //#endregion

    //#region the facilities

    private void readFacilities(ExperimentModel experiment, ExperimentSnapshot snapshot, Labels labels)
            throws Exception {
        List<URI> uris = urisOf(experiment.getFacilities());
        if (uris.isEmpty()) {
            return;
        }
        FacilityLogic logic = new FacilityLogic(sparql, nosql, user, fs);
        for (FacilityModel facility : logic.getList(uris, user)) {
            Double[] centroid = centroid(logic.getLastFacilityLocationModel(facility));
            String town = facility.getAddress() == null ? null : facility.getAddress().getLocality();
            snapshot.getFacilities().add(new Facility(expanded(facility.getUri()), facility.getName(), town,
                    centroid[1], centroid[0], otherProperties(facility, labels)));
        }
        snapshot.getFacilities().sort(Comparator.comparing(Facility::name, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * The centroid of where the facility is, longitude first as GeoJSON writes it; nothing when
     * the facility has no location. STAR asks for a centroid, and a field is usually a polygon.
     */
    private Double[] centroid(LocationObservationModel location) {
        if (location == null || location.getLocation() == null || location.getLocation().getGeometry() == null) {
            return new Double[]{null, null};
        }
        try {
            Point point = new WKTReader().read(GeospatialDAO.geometryToWkt(location.getLocation().getGeometry()))
                    .getCentroid();
            return new Double[]{point.getX(), point.getY()};
        } catch (Exception e) {
            LOGGER.warn("Could not read the location of {}", location.getFeatureOfInterest(), e);
            return new Double[]{null, null};
        }
    }

    /**
     * The custom properties of a facility, as its type gives them, named by the last segment of
     * the property: {@code row_spacing} lands in the column of that name when the instance's
     * ontology calls it so.
     */
    private Map<String, String> otherProperties(FacilityModel facility, Labels labels) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Map.Entry<URI, String> property : relations(facility, Set.of(RDF.type.getURI(), RDFS.label.getURI(),
                RDFS.comment.getURI()), labels).entrySet()) {
            properties.put(localName(property.getKey()), property.getValue());
        }
        return properties;
    }

    //#endregion

    //#region the objects

    /**
     * The objects of the experiment, grouped by type, with the platform's scientific-object search
     * — the one its CSV export runs, which brings the factor levels and every custom property.
     */
    private void readObjects(URI experiment, Map<URI, String> levelNames, ExperimentSnapshot snapshot,
                             Labels labels) throws Exception {
        List<ScientificObjectModel> objects = searchObjects(experiment);
        if (objects.isEmpty()) {
            return;
        }
        Map<URI, String> namesByUri = new HashMap<>();
        objects.forEach(object -> namesByUri.put(expanded(object.getUri()), object.getName()));

        StringUriMap<MoveModel> moves = new MoveLogic(sparql, nosql, user, fs).getInitialMovesWithLocationPerTarget(
                objects.stream().map(ScientificObjectModel::getUri).collect(Collectors.toList()), experiment);

        Map<URI, List<ScientificObjectModel>> byType = new LinkedHashMap<>();
        objects.stream()
                .sorted(Comparator.comparing(SPARQLNamedResourceModel::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(object -> byType.computeIfAbsent(expanded(object.getType()), type -> new ArrayList<>()).add(object));

        for (Map.Entry<URI, List<ScientificObjectModel>> type : byType.entrySet()) {
            List<ExportedObject> exported = new ArrayList<>();
            Map<URI, String> propertyNames = new LinkedHashMap<>();
            for (ScientificObjectModel object : type.getValue()) {
                exported.add(exported(object, levelNames, namesByUri, moves, propertyNames, labels));
            }
            ScientificObjectModel first = type.getValue().get(0);
            String label = first.getTypeLabel() == null || first.getTypeLabel().getDefaultValue() == null
                    ? localName(type.getKey())
                    : first.getTypeLabel().getDefaultValue();
            snapshot.getObjectTypes().add(new ObjectType(type.getKey(), localName(type.getKey()), label, exported,
                    propertyNames));
        }
    }

    private List<ScientificObjectModel> searchObjects(URI experiment) throws Exception {
        ScientificObjectDAO dao = new ScientificObjectDAO(sparql);
        List<ScientificObjectModel> objects = new ArrayList<>();
        for (int page = 0; ; page++) {
            ScientificObjectSearchFilter filter = new ScientificObjectSearchFilter().setExperiment(experiment);
            filter.setLang(user.getLanguage()).setPage(page).setPageSize(PAGE_SIZE);
            ListWithPagination<ScientificObjectModel> results = dao.search(filter,
                    Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD));
            objects.addAll(results.getList());
            if (results.getList().size() < PAGE_SIZE || objects.size() >= results.getTotal()) {
                return objects;
            }
        }
    }

    private ExportedObject exported(ScientificObjectModel object, Map<URI, String> levelNames,
                                    Map<URI, String> namesByUri, StringUriMap<MoveModel> moves,
                                    Map<URI, String> propertyNames, Labels labels) throws Exception {
        List<String> levels = new ArrayList<>();
        if (object.getFactorLevels() != null) {
            for (FactorLevelModel level : object.getFactorLevels()) {
                String name = level.getName() != null ? level.getName() : levelNames.get(expanded(level.getUri()));
                if (name != null) {
                    levels.add(name);
                }
            }
        }
        String parent = object.getParent() == null ? null
                : namesByUri.getOrDefault(expanded(object.getParent().getUri()), object.getParent().getName());

        String x = null;
        String y = null;
        MoveModel move = moves.get(object.getUri());
        if (move != null && move.getLocationObservation() != null && move.getLocationObservation().getLocation() != null) {
            LocationModel location = move.getLocationObservation().getLocation();
            x = location.getX();
            y = location.getY();
        }

        Map<URI, String> properties = relations(object, OBJECT_PROPERTIES_HANDLED, labels);
        properties.keySet().forEach(property -> propertyNames.computeIfAbsent(property,
                key -> uniqueColumn(localName(key), propertyNames.values())));

        return new ExportedObject(expanded(object.getUri()), object.getName(),
                joined(labels.namesOf(relationValues(object, Oeso.hasGermplasm.getURI()))), levels, parent, x, y,
                joined(literalValues(object, RDFS.comment.getURI())), properties);
    }

    //#endregion

    //#region the observations

    /**
     * Every value recorded in the experiment, page by page, each tied to what it was measured on.
     */
    private void readObservations(URI experiment, ExperimentSnapshot snapshot) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        List<URI> experiments = List.of(experiment);
        int count = dao.count(user, experiments, null, null, null, null, null, null, null, null, null, null);
        if (count > MAX_OBSERVATIONS) {
            throw new TooManyObservationsException(count);
        }
        Set<URI> known = new LinkedHashSet<>();
        snapshot.getFacilities().forEach(facility -> known.add(facility.uri()));
        snapshot.getObjectTypes().forEach(type -> type.objects().forEach(object -> known.add(object.uri())));

        int withoutTarget = 0;
        for (int page = 0; page * PAGE_SIZE < count; page++) {
            ListWithPagination<DataModel> results = dao.search(user, experiments, null, null, null, null, null,
                    null, null, null, null, null, null, page, PAGE_SIZE);
            for (DataModel data : results.getList()) {
                URI target = data.getTarget() == null ? null : expanded(data.getTarget());
                if (target == null || !known.contains(target)) {
                    withoutTarget++;
                    continue;
                }
                snapshot.getObservations().add(new Observation(target, expanded(data.getVariable()),
                        localDate(data.getDate(), data.getOffset()), !Boolean.TRUE.equals(data.getIsDateTime()),
                        data.getValue()));
            }
            if (results.getList().isEmpty()) {
                break;
            }
        }
        snapshot.setObservationsWithoutTarget(withoutTarget);
    }

    /**
     * The variables the observations use, as the dictionary describes them: the alternative name
     * as the code, since it is the short code a STAR file uses, unless two variables share it.
     */
    private void readVariables(ExperimentSnapshot snapshot) throws Exception {
        Set<URI> used = snapshot.getObservations().stream().map(Observation::variable)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (used.isEmpty()) {
            return;
        }
        List<VariableModel> variables = new VariableDAO(sparql, nosql, fs, user)
                .getList(new ArrayList<>(used), user.getLanguage());
        Map<String, Long> codes = variables.stream()
                .map(ExperimentSnapshotReader::shortCode)
                .collect(Collectors.groupingBy(code -> code.toLowerCase(), Collectors.counting()));
        Map<URI, String> units = unitsBySymbol(variables);
        for (VariableModel variable : variables) {
            String code = shortCode(variable);
            if (codes.get(code.toLowerCase()) > 1) {
                code = variable.getName();
            }
            String unit = variable.getUnit() == null ? null : units.get(expanded(variable.getUnit().getUri()));
            String term = variable.getExactMatch() == null || variable.getExactMatch().isEmpty()
                    ? expanded(variable.getUri()).toString()
                    : variable.getExactMatch().get(0).toString();
            snapshot.getVariables().put(expanded(variable.getUri()), new Variable(code, variable.getDescription(),
                    variable.getCharacteristic() == null ? null : variable.getCharacteristic().getName(),
                    variable.getMethod() == null ? null : variable.getMethod().getName(),
                    unit,
                    variable.getDataType() == null ? null : SPARQLDeserializers.getExpandedURI(variable.getDataType()),
                    term));
        }
    }

    /**
     * The unit of each variable as STAR writes it — {@code %}, {@code mm} — its symbol when it has
     * one, its name otherwise. Read from the units themselves: the variable list brings their names
     * and not their symbols.
     */
    private Map<URI, String> unitsBySymbol(List<VariableModel> variables) throws Exception {
        List<URI> uris = variables.stream().map(VariableModel::getUnit).filter(Objects::nonNull)
                .map(UnitModel::getUri).filter(Objects::nonNull).map(ExperimentSnapshotReader::expanded)
                .distinct().collect(Collectors.toList());
        Map<URI, String> units = new HashMap<>();
        if (uris.isEmpty()) {
            return units;
        }
        for (UnitModel unit : sparql.getListByURIs(UnitModel.class, uris, user.getLanguage())) {
            String symbol = unit.getSymbol();
            units.put(expanded(unit.getUri()), symbol == null || symbol.isBlank() ? unit.getName() : symbol);
        }
        return units;
    }

    private static String shortCode(VariableModel variable) {
        String alternative = variable.getAlternativeName();
        return alternative == null || alternative.isBlank() ? variable.getName() : alternative;
    }

    /**
     * The date as it was recorded, in the offset it was recorded in: a reading taken at 08:00 in
     * Montpellier is written 08:00, not the 06:00 it is in UTC.
     */
    static LocalDateTime localDate(Instant date, String offset) {
        ZoneOffset zone = ZoneOffset.UTC;
        if (offset != null && !offset.isBlank()) {
            try {
                zone = ZoneOffset.of(offset);
            } catch (RuntimeException e) {
                LOGGER.debug("Unreadable offset {}, UTC assumed", offset);
            }
        }
        return LocalDateTime.ofInstant(date, zone);
    }

    //#endregion

    //#region relations and labels

    /**
     * The custom properties of a resource, values turned into names where they name a resource.
     */
    private Map<URI, String> relations(SPARQLResourceModel resource, Set<String> excluded, Labels labels)
            throws Exception {
        Map<URI, List<String>> values = new LinkedHashMap<>();
        if (resource.getRelations() == null) {
            return new LinkedHashMap<>();
        }
        for (SPARQLModelRelation relation : resource.getRelations()) {
            if (relation.getReverse() || relation.getValue() == null) {
                continue;
            }
            String property = SPARQLDeserializers.getExpandedURI(relation.getProperty().getURI());
            if (excluded.contains(property)) {
                continue;
            }
            values.computeIfAbsent(URI.create(property), key -> new ArrayList<>()).add(relation.getValue());
        }
        Map<URI, String> properties = new LinkedHashMap<>();
        for (Map.Entry<URI, List<String>> property : values.entrySet()) {
            List<String> named = new ArrayList<>();
            for (String value : property.getValue()) {
                named.add(labels.nameOrValue(value));
            }
            properties.put(property.getKey(), joined(named));
        }
        return properties;
    }

    private static List<URI> relationValues(SPARQLResourceModel resource, String property) {
        List<URI> uris = new ArrayList<>();
        for (String value : literalValues(resource, property)) {
            try {
                uris.add(URI.create(value));
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Not a URI: {}", value);
            }
        }
        return uris;
    }

    private static List<String> literalValues(SPARQLResourceModel resource, String property) {
        List<String> values = new ArrayList<>();
        if (resource.getRelations() == null) {
            return values;
        }
        for (SPARQLModelRelation relation : resource.getRelations()) {
            if (!relation.getReverse() && relation.getValue() != null
                    && SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), property)) {
                values.add(relation.getValue());
            }
        }
        return values;
    }

    /**
     * Names by URI, asked once per URI whatever the number of rows naming it.
     */
    private final class Labels {

        private final Map<URI, String> cache = new HashMap<>();

        List<String> namesOf(Collection<URI> uris) throws Exception {
            List<URI> unknown = uris.stream().map(ExperimentSnapshotReader::expanded)
                    .filter(uri -> !cache.containsKey(uri)).distinct().collect(Collectors.toList());
            if (!unknown.isEmpty()) {
                fetch(unknown);
            }
            return uris.stream().map(uri -> cache.get(expanded(uri))).filter(Objects::nonNull)
                    .distinct().collect(Collectors.toList());
        }

        /**
         * A value naming a resource becomes that resource's name; any other value stays as written.
         */
        String nameOrValue(String value) throws Exception {
            if (!looksLikeUri(value)) {
                return value;
            }
            List<String> names = namesOf(List.of(URI.create(value)));
            return names.isEmpty() ? value : names.get(0);
        }

        private void fetch(List<URI> uris) throws Exception {
            uris.forEach(uri -> cache.put(uri, null));
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
            SelectBuilder select = new SelectBuilder().addVar(uriVar).addVar(nameVar).setDistinct(true);
            select.addWhere(uriVar, RDFS.label, nameVar);
            select.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, uris));
            select.addFilter(SPARQLQueryHelper.langFilterWithDefault(nameVar.getVarName(), user.getLanguage()));
            for (SPARQLResult result : sparql.executeSelectQuery(select)) {
                URI uri = expanded(URI.create(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
                if (cache.get(uri) == null) {
                    cache.put(uri, result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));
                }
            }
        }
    }

    //#endregion

    //#region helpers

    private static List<URI> urisOf(List<? extends SPARQLResourceModel> resources) {
        if (resources == null) {
            return new ArrayList<>();
        }
        return resources.stream().filter(Objects::nonNull).map(SPARQLResourceModel::getUri)
                .filter(Objects::nonNull).map(ExperimentSnapshotReader::expanded)
                .distinct().collect(Collectors.toList());
    }

    static URI expanded(URI uri) {
        return uri == null ? null : URI.create(SPARQLDeserializers.getExpandedURI(uri));
    }

    /**
     * The last segment of a URI, after its {@code #} or its last {@code /}.
     */
    static String localName(URI uri) {
        String text = uri.toString();
        int cut = Math.max(text.lastIndexOf('#'), Math.max(text.lastIndexOf('/'), text.lastIndexOf(':')));
        return cut >= 0 && cut < text.length() - 1 ? text.substring(cut + 1) : text;
    }

    private static String uniqueColumn(String wanted, Collection<String> taken) {
        String column = wanted;
        for (int i = 2; taken.contains(column); i++) {
            column = wanted + "_" + i;
        }
        return column;
    }

    private static boolean looksLikeUri(String value) {
        return value.startsWith("http://") || value.startsWith("https://")
                || (value.contains(":") && !value.contains(" ") && !value.matches("^[0-9.:\\-+TZ]+$"));
    }

    private static String joined(List<String> values) {
        return values.isEmpty() ? null : String.join(" ", values);
    }

    //#endregion

    /**
     * An experiment too large for a workbook.
     */
    public static class TooManyObservationsException extends Exception {

        private final int count;

        public TooManyObservationsException(int count) {
            super(count + " values exceed the " + MAX_OBSERVATIONS + " a STAR workbook export carries");
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }
}
