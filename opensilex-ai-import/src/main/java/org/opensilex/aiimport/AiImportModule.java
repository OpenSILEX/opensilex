//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport;

import org.opensilex.OpenSilexModule;
import org.opensilex.server.extensions.APIExtension;

/**
 * LLM-assisted import module.
 * <p>
 * Exposes a conversational assistant that reads a spreadsheet, describes what it contains and
 * checks against the instance whether the referenced variables, projects, experiments, germplasm
 * and scientific objects already exist. This module never writes to the databases.
 *
 * @author Arnaud Charleroy
 */
public class AiImportModule extends OpenSilexModule implements APIExtension {

    @Override
    public Class<?> getConfigClass() {
        return AiImportConfig.class;
    }

    @Override
    public String getConfigId() {
        return "ai-import";
    }
}
