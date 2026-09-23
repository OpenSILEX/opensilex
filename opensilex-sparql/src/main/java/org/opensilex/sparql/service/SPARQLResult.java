//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 *
 * @author Vincent Migot
 */
public interface SPARQLResult {

    String getStringValue(String key);

    Optional<SPARQLLiteral> getLiteralValue(String key);

    boolean isURI(String key);

    boolean isLiteral(String key);
    
    void forEach(BiConsumer<? super String, ? super String> action);
}
