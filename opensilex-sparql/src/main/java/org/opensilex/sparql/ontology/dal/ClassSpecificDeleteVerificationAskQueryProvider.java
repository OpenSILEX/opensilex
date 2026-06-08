/*
 * *****************************************************************************
 *                         ClassSpecificDeleteVerificationAskQueryProvider.java
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
 * this interface is used to let other modules provide the necessary query to ensure that a class can be deleted following specific Logic to this class.
 * See implementations for an exemple verifying that a factor category can be deleted.
 * using this interface allow us to use ClassLoader and runtime dependency injection, see usage in {@link OntologyDAO#deleteClass(URI)}.
 */
public interface ClassSpecificDeleteVerificationAskQueryProvider {
    public abstract AskBuilder getFactorCategoryDeleteVerificationAskQuery(URI factorCategoryURI);

    public abstract String getErrorTranslationKey();
}
