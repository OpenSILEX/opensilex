/*******************************************************************************
 *                         TranslatedModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.model;

public interface TranslatedModel {

    SPARQLLabel getLabel();

    void setLabel(SPARQLLabel label);

    String COMMENT_FIELD = "comment";

    SPARQLLabel getComment();

    void setComment(SPARQLLabel comment);

    String getName();

    void setName(String name);
}
