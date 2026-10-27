//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.bll;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.taxon.dal.Kingdom;
import org.opensilex.core.taxon.dal.TaxonDAO;
import org.opensilex.core.taxon.dal.TaxonModel;
import org.opensilex.core.taxon.dal.TaxonSearchFilter;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Business rules on taxa. Taxa are entered by users (no external taxonomy is queried).
 */
public class TaxonLogic {

    /**
     * Identifier convention: "SOURCE:value", e.g. "NCBITaxon:562" or "TAXREF:129968".
     * The source avoids collisions between identifiers that have the same number in two sources.
     */
    public static final Pattern TAXON_ID_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]*:\\S+$");

    private static final URI SPECIES = URI.create(Oeso.Species.getURI());
    private static final URI GENUS = URI.create(Oeso.Genus.getURI());

    private final TaxonDAO dao;
    private final AccountModel currentUser;

    public TaxonLogic(SPARQLService sparql, AccountModel currentUser) {
        this.dao = new TaxonDAO(sparql);
        this.currentUser = currentUser;
    }

    private String lang() {
        return currentUser != null ? currentUser.getLanguage() : null;
    }

    // ------------------------------------------------------------------ read

    public TaxonModel get(URI uri) throws Exception {
        TaxonModel taxon = dao.get(uri, lang());
        if (taxon == null) {
            throw new NotFoundURIException("Taxon not found", uri);
        }
        return taxon;
    }

    public List<TaxonModel> getList(Collection<URI> uris) throws Exception {
        return dao.getList(uris, lang());
    }

    public ListWithPagination<TaxonModel> search(TaxonSearchFilter filter) throws Exception {
        filter.setLang(lang());
        return dao.search(filter);
    }

    public List<Kingdom> getKingdoms() throws Exception {
        return dao.getKingdoms(lang());
    }

    // ------------------------------------------------------------------ write

    public TaxonModel create(TaxonModel taxon) throws Exception {
        normalize(taxon);
        check(taxon);
        return dao.create(taxon);
    }

    public TaxonModel update(TaxonModel taxon) throws Exception {
        if (taxon.getUri() == null) {
            throw new BadRequestException("The taxon URI is mandatory for an update");
        }
        TaxonModel existing = get(taxon.getUri());
        normalize(taxon);
        check(taxon);
        checkKingdomChange(existing, taxon);
        return dao.update(taxon);
    }

    public void delete(URI uri) throws Exception {
        get(uri);
        if (dao.isUsed(uri)) {
            throw new ConflictException("This taxon is used (genetic resource, scientific object, experiment, variable or species of a genus) and cannot be deleted");
        }
        dao.delete(uri);
    }

    // ------------------------------------------------------------------ rules

    /**
     * Trims the identifiers, removes the empty ones and the duplicates (order kept).
     */
    private void normalize(TaxonModel taxon) {
        if (taxon.getTaxonId() == null) {
            taxon.setTaxonId(new ArrayList<>());
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String id : taxon.getTaxonId()) {
            String trimmed = StringUtils.trimToNull(id);
            if (trimmed != null) {
                ids.add(trimmed);
            }
        }
        taxon.setTaxonId(new ArrayList<>(ids));
    }

    void check(TaxonModel taxon) throws Exception {
        if (StringUtils.isBlank(taxon.getName())) {
            throw new BadRequestException("The scientific name is mandatory");
        }
        if (!isSpecies(taxon) && !isGenus(taxon)) {
            throw new BadRequestException("A taxon must be a Species or a Genus");
        }
        if (taxon.getKingdom() == null || !dao.isKingdom(taxon.getKingdom(), lang())) {
            throw new BadRequestException("A taxon must belong to one of the kingdoms of the ontology");
        }
        for (String taxonId : taxon.getTaxonId()) {
            if (!TAXON_ID_PATTERN.matcher(taxonId).matches()) {
                throw new BadRequestException("Identifier '" + taxonId + "' must be prefixed by its source, e.g. NCBITaxon:562 or TAXREF:129968");
            }
            Optional<TaxonModel> other = dao.findByTaxonId(taxonId, lang());
            if (other.isPresent() && (taxon.getUri() == null || !SPARQLDeserializers.compareURIs(other.get().getUri(), taxon.getUri()))) {
                throw new ConflictException("Identifier " + taxonId + " is already used by " + other.get().getUri());
            }
        }
        if (taxon.getGenus() != null) {
            if (!isSpecies(taxon)) {
                throw new BadRequestException("Only a species can have a genus");
            }
            TaxonModel genus = dao.get(taxon.getGenus(), lang());
            if (genus == null || !isGenus(genus)) {
                throw new BadRequestException("The genus of a species must be an existing Genus");
            }
            if (genus.getKingdom() != null && !SPARQLDeserializers.compareURIs(genus.getKingdom(), taxon.getKingdom())) {
                throw new BadRequestException("A species and its genus must belong to the same kingdom");
            }
        }
    }

    /**
     * The kingdom drives the rules of everything attached to the taxon (a variety must be a plant, the species
     * of a genus must be in the same kingdom...). Changing the kingdom of a used taxon is refused.
     */
    private void checkKingdomChange(TaxonModel existing, TaxonModel updated) throws Exception {
        boolean kingdomChanged = existing.getKingdom() != null
                && !SPARQLDeserializers.compareURIs(existing.getKingdom(), updated.getKingdom());
        if (kingdomChanged && dao.isUsed(updated.getUri())) {
            throw new ConflictException("This taxon is used: detach the resources attached to it before changing its kingdom");
        }
    }

    private static boolean isSpecies(TaxonModel taxon) {
        return taxon.getType() != null && SPARQLDeserializers.compareURIs(taxon.getType(), SPECIES);
    }

    private static boolean isGenus(TaxonModel taxon) {
        return taxon.getType() != null && SPARQLDeserializers.compareURIs(taxon.getType(), GENUS);
    }
}
