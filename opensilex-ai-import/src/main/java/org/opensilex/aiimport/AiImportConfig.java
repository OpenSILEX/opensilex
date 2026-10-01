//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport;

import org.opensilex.aiimport.config.LlmConfig;
import org.opensilex.config.ConfigDescription;

/**
 * Configuration of the {@code ai-import} module, read from the {@code ai-import} key of
 * {@code opensilex.yml}.
 *
 * @author Arnaud Charleroy
 */
public interface AiImportConfig {

    @ConfigDescription(
            value = "Enable the LLM-assisted import assistant",
            defaultBoolean = false
    )
    boolean enabled();

    @ConfigDescription(value = "OpenAI-compatible chat completion endpoint used by the assistant")
    LlmConfig llm();

    @ConfigDescription(
            value = "Maximum size, in megabytes, of an uploaded workbook",
            defaultInt = 20
    )
    int maxFileSizeMb();

    @ConfigDescription(
            value = "Number of sample rows per sheet sent to the language model",
            defaultInt = 5
    )
    int sampleRowsPerSheet();

    @ConfigDescription(
            value = "Lifetime, in minutes, of an import conversation held in memory",
            defaultInt = 60
    )
    int sessionTtlMinutes();

    @ConfigDescription(
            value = "Days a stored import conversation is kept after its last activity, before it and "
                    + "its file are deleted",
            defaultInt = 30
    )
    int savedSessionDays();

    @ConfigDescription(
            value = "Also look up missing variables on the configured shared resource instances",
            defaultBoolean = true
    )
    boolean searchSharedResourceInstances();
}
