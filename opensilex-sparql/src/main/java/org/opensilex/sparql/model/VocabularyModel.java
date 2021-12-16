/*******************************************************************************
 *                         VocabularyModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.model;

public interface VocabularyModel<T extends VocabularyModel<T>> extends SPARQLModel, TranslatedModel, DagModel<T> {

}
