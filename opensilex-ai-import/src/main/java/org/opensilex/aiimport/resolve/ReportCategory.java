//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.core.organisation.api.facility.FacilityAPI;
import org.opensilex.core.project.api.ProjectAPI;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.organisation.api.OrganizationAPI;
import org.opensilex.security.person.api.PersonAPI;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The kinds of resource the report lists, named once.
 * <p>
 * The key is the one the interface uses for the same category, so a confirmation sent from a
 * report row names its category without a translation table on either side.
 *
 * @author Arnaud Charleroy
 */
public enum ReportCategory {

    EXPERIMENTS("experiments", true, ResolutionReport::getExperiments,
            ExperimentAPI.CREDENTIAL_EXPERIMENT_MODIFICATION_ID),
    PROJECTS("projects", true, ResolutionReport::getProjects,
            ProjectAPI.CREDENTIAL_PROJECT_MODIFICATION_ID),
    VARIABLES("variables", true, ResolutionReport::getVariables,
            VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID),
    GERMPLASM("germplasm", true, ResolutionReport::getGermplasm,
            GermplasmAPI.CREDENTIAL_GERMPLASM_MODIFICATION_ID),

    /**
     * Excluded from near matching. Plot codes such as {@code A1}, {@code A10} and {@code 1A} are
     * one or two edits apart and name different plots, so a distance says nothing about them. The
     * one known mismatch between two ways of writing them is handled by a deterministic
     * reconciliation, which says exactly what it did.
     */
    SCIENTIFIC_OBJECTS("scientificObjects", false, ResolutionReport::getScientificObjects, null),

    FACILITIES("facilities", true, ResolutionReport::getFacilities,
            FacilityAPI.CREDENTIAL_FACILITY_MODIFICATION_ID),
    PERSONS("persons", true, ResolutionReport::getPersons,
            PersonAPI.CREDENTIAL_PERSON_MODIFICATION_ID),

    /**
     * The institution and the unit that run the trial, a unit being part of its institution.
     */
    ORGANIZATIONS("organizations", true, ResolutionReport::getOrganizations,
            OrganizationAPI.CREDENTIAL_ORGANIZATION_MODIFICATION_ID);

    private final String key;
    private final boolean nearMatching;
    private final Function<ResolutionReport, List<ResolvedItem>> items;

    /**
     * The right needed to teach the instance a misspelling of this kind. A taught correction acts
     * on every later import, by anyone, so it takes the same right as editing the resource itself.
     */
    private final String modificationCredential;

    ReportCategory(String key, boolean nearMatching,
                   Function<ResolutionReport, List<ResolvedItem>> items,
                   String modificationCredential) {
        this.key = key;
        this.nearMatching = nearMatching;
        this.items = items;
        this.modificationCredential = modificationCredential;
    }

    /**
     * @return the credential needed to teach a correction, null when none can be taught
     */
    public String getModificationCredential() {
        return modificationCredential;
    }

    public String getKey() {
        return key;
    }

    /**
     * @return whether a misspelt name of this kind can be recognised by its spelling
     */
    public boolean allowsNearMatching() {
        return nearMatching;
    }

    public List<ResolvedItem> itemsOf(ResolutionReport report) {
        return items.apply(report);
    }

    public static Optional<ReportCategory> fromKey(String key) {
        for (ReportCategory category : values()) {
            if (category.key.equals(key)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
