/*
 * *****************************************************************************
 *                         OpenApiExtension.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *****************************************************************************
 */

package org.opensilex;

import java.util.List;

/**
 * Interface allowing OpenSILEX modules to register additional DTO models
 * into the generated OpenAPI 3.1 schema components.
 */
public interface OpenApiExtension {
    List<Class<?>> getAdditionalOpenApiDefinitions();
}
