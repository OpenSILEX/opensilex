/*
 * *****************************************************************************
 *                         FactorLevelDeleteVerificationAskQueryProvider.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 29/05/2026 10:20
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.sparql.ontology.dal;

import org.apache.jena.arq.querybuilder.AskBuilder;

import java.net.URI;

/**
 * this interface is used to let the opensilex-core module provide the necessary query to ensure that a factor category can be deleted.
 * using this interface allow us to use ClassLoader and runtime dependency injection, because the opensilex-sparql module can't depend on the opensilex-core module.
 */
public interface FactorCategoryDeleteVerificationAskQueryProvider {
    public abstract AskBuilder getFactorCategoryDeleteVerificationAskQuery(URI factorCategoryURI);
}
