//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.HeaderMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a column header onto a business entity, from the header alone.
 * <p>
 * This is the mapping of last resort, and the one that has to work when nothing else does: no known
 * template, and no language model either. The assistant is a conversation, not a dependency — a
 * user whose model endpoint is down still uploads a file and still needs to be told what its
 * columns are. Everything here is a table lookup.
 * <p>
 * The vocabulary is deliberately drawn from the three families the module already knows, in both
 * languages, because those are the words agronomic spreadsheets actually use. A profile that
 * recognises its template overrides this with something better; this is what is left when none
 * does.
 * <p>
 * Two rules keep it honest. A term matches the <em>whole</em> normalised header before it matches a
 * part of it, so {@code plot_id} is an object and {@code plot_area} is not mistaken for one. And a
 * word too generic to decide on — {@code x}, {@code code}, {@code name} — is absent from the table
 * rather than guessed at: a column left {@code UNKNOWN} asks a question, where a column mapped
 * wrongly answers one that was never asked.
 *
 * @author Arnaud Charleroy
 */
public final class HeaderRoleDictionary {

    /**
     * Role by term, in insertion order: the first role whose vocabulary matches wins, so the more
     * specific families come first.
     * <p>
     * The terms are normalised the way {@link HeaderMatcher#normalize} normalises a header —
     * lowercase, unaccented, no separators — so {@code Nom_Parcelle} and {@code nom parcelle} meet
     * the same entry.
     */
    private static final Map<ColumnRole, List<String>> VOCABULARY = buildVocabulary();

    private HeaderRoleDictionary() {
    }

    private static Map<ColumnRole, List<String>> buildVocabulary() {
        Map<ColumnRole, List<String>> vocabulary = new LinkedHashMap<>();

        // The observed object. 'parcelle' is absent on purpose: STAR calls a field a parcelle and
        // treats it as a facility, while other templates use it for the observed plot. A word that
        // means two things in two templates cannot be resolved here.
        vocabulary.put(ColumnRole.OBJECT, Arrays.asList(
                "plotid", "plot", "placette", "placetteid", "uniteexperimentale", "ue",
                "observationunitid", "observationunit", "ouid", "objetid", "objet",
                "scientificobject", "scientificobjectid", "objecturi", "pu", "puid",
                "plantid", "plante", "potid", "arbreid", "treeid", "souche", "cep"));

        vocabulary.put(ColumnRole.TRIAL, Arrays.asList(
                "experimentation", "experiment", "experimentid", "essai", "essaiid", "dispositif",
                "trial", "trialid", "study", "studyid", "studyuniqueid", "etude", "expeid",
                "experimenturi"));

        vocabulary.put(ColumnRole.PROJECT, Arrays.asList(
                "projet", "projetid", "project", "projectid", "projid", "programme", "program",
                "investigation", "investigationtitle", "investigationuniqueid"));

        vocabulary.put(ColumnRole.GERMPLASM, Arrays.asList(
                "genotype", "genotypeid", "cultivar", "cultivarname", "variete", "varietyname",
                "variety", "accession", "accessionnumber", "germplasm", "germplasmid",
                "materielvegetal", "biologicalmaterial", "biologicalmaterialid", "espece",
                "species", "porte greffe", "portegreffe", "rootstock"));

        // A position is dated in OpenSILEX — it is a move event, not an attribute — because a pot
        // moves and a plot does not, and the model does not distinguish the two in advance.
        vocabulary.put(ColumnRole.POSITION, Arrays.asList(
                "plotx", "ploty", "plotz", "positionx", "positiony", "x", "y", "z",
                "textualposition", "positiondescription"));

        // A group the objects belong to. Whether it becomes an object or a property is the user's
        // call, not this table's — see ColumnRole.PARENT_OBJECT.
        vocabulary.put(ColumnRole.PARENT_OBJECT, Arrays.asList(
                "bloc", "bloccode", "blockcode", "block", "blocid", "blockid", "sousbloc",
                "subblock"));

        // What the observed object is like, as opposed to what was measured on it. A property is
        // written once on the object; a measurement is written per observation, with a date and a
        // provenance. The words here name a layout or a planting, never a reading.
        vocabulary.put(ColumnRole.OBJECT_PROPERTY, Arrays.asList(
                "premierrang", "dernierrang", "premieresouche", "dernieresouche",
                "rowspacing", "plantspacing", "interrang", "ecartement", "espacement",
                "densitedeplantation", "densiteplantation", "plantingdensity",
                "orientationdesrangs", "orientationrangs", "nombredesouches", "nbsouches"));

        // Where the trial sits: a facility, or the coordinates of one.
        vocabulary.put(ColumnRole.LOCATION, Arrays.asList(
                "site", "sitename", "experimentalsitename", "lieu", "lieudit", "facility",
                "facilityid", "installation", "station", "fieldid", "fieldname", "champ",
                "latitude", "longitude", "altitude", "commune", "communename", "communeinseeid",
                "growthfacility", "geographiclocation"));

        // A person named as such, with an email or an ORCID, as opposed to an observer's code.
        vocabulary.put(ColumnRole.PERSON, Arrays.asList(
                "personname", "personemail", "personid", "personaffiliation", "personrole",
                "contact", "contactinstitution", "contactname", "auteur", "author", "orcid",
                "email", "courriel", "responsable", "submitter"));

        vocabulary.put(ColumnRole.OBSERVER, Arrays.asList(
                "observateur", "observer", "operateur", "operator", "agent", "notateur",
                "saisipar", "measuredby", "pappoperator", "poperator"));

        vocabulary.put(ColumnRole.SEASON, Arrays.asList(
                "campagne", "saison", "season", "millesime", "annee", "year", "harvestyear"));

        vocabulary.put(ColumnRole.FACTOR_LEVEL, Arrays.asList(
                "modalite", "modaliteid", "traitement", "treatment", "xptrtcode", "xptrtname",
                "facteur", "factor", "factortype", "factorvalue", "experimentalfactortype",
                "repetition", "rep", "repet"));

        vocabulary.put(ColumnRole.COMMENT, Arrays.asList(
                "commentaire", "comment", "comments", "remarque", "remarques", "note", "notes",
                "notelibre", "obslibre", "description", "observationlibre"));

        return Collections.unmodifiableMap(vocabulary);
    }

    /**
     * @param header the column header, as written in the file
     * @return the business entity the header names, or {@link ColumnRole#UNKNOWN}
     */
    public static ColumnRole roleOf(String header) {
        if (header == null || header.trim().isEmpty()) {
            return ColumnRole.UNKNOWN;
        }
        // A date is recognised by the parser rather than by this table, since it already knows the
        // forms a date column takes and is used for the same decision when reading the values.
        if (ExcelValueParser.looksLikeDateColumn(header)) {
            return ColumnRole.DATE;
        }

        String normalised = HeaderMatcher.normalize(header);
        if (normalised.isEmpty()) {
            return ColumnRole.UNKNOWN;
        }

        // The whole header first: 'plot' is an object, 'plotarea' is a measurement of one.
        for (Map.Entry<ColumnRole, List<String>> entry : VOCABULARY.entrySet()) {
            if (entry.getValue().contains(normalised)) {
                return entry.getKey();
            }
        }
        // Then a prefix or suffix, which is how a template qualifies a known column: 'id_placette',
        // 'placette_2024'. A term found in the middle is not enough — 'surface_bloc_m2' measures
        // something, it does not name a block.
        for (Map.Entry<ColumnRole, List<String>> entry : VOCABULARY.entrySet()) {
            for (String term : entry.getValue()) {
                if (term.length() >= MIN_AFFIX_LENGTH
                        && (normalised.startsWith(term) || normalised.endsWith(term))) {
                    return entry.getKey();
                }
            }
        }
        return ColumnRole.UNKNOWN;
    }

    /**
     * Below this, a term is too short to be recognised at the edge of a longer header without
     * catching unrelated words — {@code ue} would match {@code residue}.
     */
    private static final int MIN_AFFIX_LENGTH = 4;
}
