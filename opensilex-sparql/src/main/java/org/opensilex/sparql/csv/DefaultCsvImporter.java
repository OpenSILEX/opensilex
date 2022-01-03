/*******************************************************************************
 *                         DefaultCsvIm.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.csv;

import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.function.Supplier;

public class DefaultCsvImporter<T  extends SPARQLResourceModel> extends AbstractCsvImporter<T>{

    public DefaultCsvImporter(SPARQLService sparql, Class<T> objectClass, URI graph, Supplier<T> objectConstructor) {
        super(sparql, objectClass, graph, objectConstructor);
    }
}
