//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport;

import org.opensilex.config.ConfigDescription;

/**
 * Configuration of the {@code graph-import} module, read from the {@code graph-import} key of
 * {@code opensilex.yml}.
 *
 * @author Arnaud Charleroy
 */
public interface GraphImportConfig {

    @ConfigDescription(
            value = "Enable the experimental import assistant",
            defaultBoolean = true
    )
    boolean enabled();

    @ConfigDescription(
            value = "Let the assistant create resources in the instance (step 4); off, the step only shows what it would create",
            defaultBoolean = false
    )
    boolean creationEnabled();

    @ConfigDescription(
            value = "Maximum size, in megabytes, of an uploaded workbook or CSV file",
            defaultInt = 20
    )
    int maxFileSizeMb();

    @ConfigDescription(
            value = "Lifetime, in minutes, of an import held in memory",
            defaultInt = 60
    )
    int sessionTtlMinutes();

    @ConfigDescription(
            value = "Days a stored import is kept after its last activity, before it and its files are deleted",
            defaultInt = 30
    )
    int savedSessionDays();
}
