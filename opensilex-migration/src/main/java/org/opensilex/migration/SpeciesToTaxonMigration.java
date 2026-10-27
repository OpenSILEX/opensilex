//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.migration.geneticresource;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.service.SPARQLLiteral;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Step 1 of the genetic resources generalization: turns the existing species into valid taxa,
 * WITHOUT changing their URIs (AGROVOC URIs of the default species are kept). No external taxonomy is queried.
 *
 * <p>For each instance of oeso:Species, in its own graph:</p>
 * <ul>
 *     <li>adds oeso:belongsToKingdom oeso:Plantae if no kingdom (legacy species are plants);</li>
 *     <li>adds a label without language, copied from the Latin label (scientific name only, whatever the
 *     language of the user). Existing labels are kept;</li>
 *     <li>adds oeso:hasTaxonID "NCBITaxon:n" for each skos:exactMatch towards an NCBITaxon URI;</li>
 *     <li>writes a CSV report to be reviewed (names to check, genera typed as species, germplasm-only properties...).</li>
 * </ul>
 *
 * <p>Idempotent: only missing triples are added. Nothing is deleted.</p>
 *
 * <p>Run: {@code opensilex system run-update org.opensilex.migration.geneticresource.SpeciesToTaxonMigration}
 * with optional system properties:</p>
 * <ul>
 *     <li>{@code -Dtaxon.migration.dryRun=true}: report only, no write;</li>
 *     <li>{@code -Dtaxon.migration.report=/path/report.csv}: report location (default: temporary directory).</li>
 * </ul>
 */
public class SpeciesToTaxonMigration implements OpenSilexModuleUpdate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesToTaxonMigration.class);
    private static final String SKOS_EXACT_MATCH = "http://www.w3.org/2004/02/skos/core#exactMatch";
    private static final String NCBI_TAXON_PREFIX = "http://purl.obolibrary.org/obo/NCBITaxon_";
    /** Prefix of the identifiers in oeso:hasTaxonID (same convention as TaxonLogic). */
    private static final String NCBI_ID_PREFIX = "NCBITaxon:";
    private static final String LATIN_LANG = "la";

    /** Properties expected on a taxon: any other one is reported (germplasm-only attributes on a species). */
    private static final Set<String> EXPECTED_PROPERTIES = Set.of(
            RDF.type.getURI(), RDFS.label.getURI(), Oeso.isPublic.getURI(),
            Oeso.belongsTo.getURI(), Oeso.hasTaxonId.getURI(), Oeso.hasGenus.getURI(), SKOS_EXACT_MATCH,
            "http://purl.org/dc/terms/publisher", "http://purl.org/dc/terms/issued", "http://purl.org/dc/terms/modified"
    );

    private OpenSilex opensilex;
    private SPARQLService sparql;

    private final boolean dryRun = Boolean.parseBoolean(System.getProperty("taxon.migration.dryRun", "false"));
    private final Path reportPath = Path.of(System.getProperty("taxon.migration.report",
            Path.of(System.getProperty("java.io.tmpdir"), "species-to-taxon-report.csv").toString()));

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.parse("2026-10-15T00:00:00Z");
    }

    @Override
    public String getDescription() {
        return "Genetic resources step 1: turn existing species into taxa (kingdom, scientific name, identifiers)";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    /** For tests: inject the SPARQL service. */
    public void setSparql(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        try {
            if (sparql == null) {
                sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
            }
            run();
        } catch (Exception e) {
            throw new OpensilexModuleUpdateException(this, e);
        }
    }

    /** Package visible for tests. */
    List<ReportLine> run() throws Exception {
        Map<String, SpeciesRow> species = readSpecies();
        Map<String, Set<String>> extraProperties = readExtraProperties();
        LOGGER.info("{} species found (dryRun={})", species.size(), dryRun);

        List<ReportLine> report = new ArrayList<>();
        StringBuilder inserts = new StringBuilder();
        for (SpeciesRow row : species.values()) {
            ReportLine line = migrate(row, extraProperties.getOrDefault(row.uri, Set.of()));
            report.add(line);
            if (!line.triples.isEmpty()) {
                inserts.append("GRAPH <").append(row.graph).append("> {\n");
                line.triples.forEach(t -> inserts.append("  ").append(t).append(" .\n"));
                inserts.append("}\n");
            }
        }

        if (!dryRun && inserts.length() > 0) {
            try {
                sparql.startTransaction();
                sparql.executeUpdateQuery("INSERT DATA {\n" + inserts + "}");
                sparql.commitTransaction();
            } catch (Exception e) {
                sparql.rollbackTransaction(e);
            }
        }
        writeReport(report);
        LOGGER.info("Species to taxon migration done. Report: {}", reportPath);
        return report;
    }

    private ReportLine migrate(SpeciesRow row, Set<String> extraProperties) {
        ReportLine line = new ReportLine(row);
        String s = "<" + row.uri + ">";

        // 1. Kingdom
        if (row.kingdom == null) {
            line.triples.add(s + " <" + Oeso.belongsTo.getURI() + "> <" + Oeso.Plantae.getURI() + ">");
            line.kingdomAfter = Oeso.Plantae.getURI();
        }

        // 2. Scientific name without language, copied from the Latin label
        String latin = row.labels.get(LATIN_LANG);
        if (latin != null && !row.labels.containsKey("")) {
            line.triples.add(s + " <" + RDFS.label.getURI() + "> \"" + escape(latin) + "\"");
        }
        if (latin == null && !row.labels.containsKey("")) {
            line.warnings.add("no Latin label: the displayed name depends on the user language");
        }

        // 3. Identifiers deduced from the links to NCBITaxon (no network)
        for (String match : row.exactMatches) {
            if (match.startsWith(NCBI_TAXON_PREFIX)) {
                String id = NCBI_ID_PREFIX + match.substring(NCBI_TAXON_PREFIX.length());
                if (!row.taxonId.contains(id)) {
                    line.triples.add(s + " <" + Oeso.hasTaxonId.getURI() + "> \"" + escape(id) + "\"");
                    line.taxonIdAdded.add(id);
                }
            }
        }
        if (row.taxonId.isEmpty() && line.taxonIdAdded.isEmpty()) {
            line.warnings.add("no identifier");
        }

        // 4. Warnings to be reviewed by a human
        String name = row.scientificName();
        if (name == null) {
            line.warnings.add("no label");
        } else {
            String[] words = name.trim().split("\\s+");
            if (words.length == 1) {
                line.warnings.add("single word name: probably a genus typed as Species");
            } else if (words.length > 2 || name.contains("×")) {
                line.warnings.add("check the name (author, hybrid sign or infraspecific rank)");
            }
        }
        if (!extraProperties.isEmpty()) {
            line.warnings.add("germplasm-only properties on a species: " + String.join(" ", extraProperties));
        }
        if (line.triples.isEmpty()) {
            line.warnings.add("already migrated");
        }
        return line;
    }

    private Map<String, SpeciesRow> readSpecies() throws Exception {
        Var g = makeVar("g"), s = makeVar("s"), label = makeVar("label"), kingdom = makeVar("kingdom"),
                taxonId = makeVar("taxonId"), match = makeVar("match");
        SelectBuilder select = new SelectBuilder().setDistinct(true)
                .addVar(g).addVar(s).addVar(label).addVar(kingdom).addVar(taxonId).addVar(match);
        select.addGraph(g, new WhereBuilder()
                .addWhere(s, RDF.type, Oeso.Species)
                .addOptional(s, RDFS.label, label)
                .addOptional(s, Oeso.belongsTo, kingdom)
                .addOptional(s, Oeso.hasTaxonId, taxonId)
                .addOptional(s, NodeFactory.createURI(SKOS_EXACT_MATCH), match));

        Map<String, SpeciesRow> rows = new LinkedHashMap<>();
        sparql.executeSelectQuery(select, result -> {
            SpeciesRow row = rows.computeIfAbsent(result.getStringValue("s"), uri -> new SpeciesRow(uri, result.getStringValue("g")));
            String value = result.getStringValue("label");
            if (value != null) {
                String lang = result.getLiteralValue("label").map(SPARQLLiteral::lang).orElse("");
                row.labels.put(lang == null ? "" : lang, value);
            }
            if (row.kingdom == null) {
                row.kingdom = result.getStringValue("kingdom");
            }
            Optional.ofNullable(result.getStringValue("taxonId")).ifPresent(row.taxonId::add);
            Optional.ofNullable(result.getStringValue("match")).ifPresent(row.exactMatches::add);
        });
        return rows;
    }

    private Map<String, Set<String>> readExtraProperties() throws Exception {
        Var g = makeVar("g"), s = makeVar("s"), p = makeVar("p"), o = makeVar("o");
        SelectBuilder select = new SelectBuilder().setDistinct(true).addVar(s).addVar(p);
        select.addGraph(g, new WhereBuilder().addWhere(s, RDF.type, Oeso.Species).addWhere(s, p, o));
        Map<String, Set<String>> extra = new HashMap<>();
        sparql.executeSelectQuery(select, result -> {
            String property = result.getStringValue("p");
            if (!EXPECTED_PROPERTIES.contains(property)) {
                String shortName = property.contains("#") ? property.substring(property.lastIndexOf('#') + 1) : property;
                extra.computeIfAbsent(result.getStringValue("s"), k -> new TreeSet<>()).add(shortName);
            }
        });
        return extra;
    }

    private void writeReport(List<ReportLine> report) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("uri;graph;scientific_name;labels;kingdom_after;taxon_ids_added;triples_added;warnings");
        for (ReportLine l : report) {
            lines.add(String.join(";",
                    l.row.uri, l.row.graph, csv(l.row.scientificName()), csv(l.row.labels.toString()),
                    csv(l.kingdomAfter != null ? l.kingdomAfter : l.row.kingdom),
                    csv(String.join(" ", l.taxonIdAdded)),
                    String.valueOf(dryRun ? 0 : l.triples.size()), csv(String.join(" | ", l.warnings))));
        }
        Files.write(reportPath, lines, StandardCharsets.UTF_8);
    }

    /** Escapes a value for a SPARQL string literal. */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String csv(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }

    static final class SpeciesRow {
        final String uri;
        final String graph;
        final Map<String, String> labels = new TreeMap<>();
        final Set<String> taxonId = new TreeSet<>();
        final Set<String> exactMatches = new TreeSet<>();
        String kingdom;

        SpeciesRow(String uri, String graph) {
            this.uri = uri;
            this.graph = graph;
        }

        /** Latin label first (AGROVOC species), then the label without language, then any label. */
        String scientificName() {
            if (labels.containsKey(LATIN_LANG)) {
                return labels.get(LATIN_LANG);
            }
            if (labels.containsKey("")) {
                return labels.get("");
            }
            return labels.values().stream().findFirst().orElse(null);
        }
    }

    static final class ReportLine {
        final SpeciesRow row;
        final List<String> triples = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> taxonIdAdded = new ArrayList<>();
        String kingdomAfter;

        ReportLine(SpeciesRow row) {
            this.row = row;
        }
    }
}