/*
 * *****************************************************************************
 *                         SwaggerExtension.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/09/2024 10:54
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex;

import java.util.List;

/**
 * This interface allows to add DTO (known as Swagger definitions) to the generated Swagger file even if this DTO is not referenced in an API service.
 * This is useful because the typescripts models are generated from the Swagger file, which does not contain super classes of DTO.
 */
public interface SwaggerExtension {
    List<Class<?>> getAdditionalSwaggerDefinitions();
}
