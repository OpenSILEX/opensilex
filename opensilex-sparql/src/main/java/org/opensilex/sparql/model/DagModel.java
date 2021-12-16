/*******************************************************************************
 *                         DagModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.model;

import java.util.List;
import java.util.Set;

public interface DagModel<T> {

     Set<T> getParents();

     void setParents(Set<T> parents);

     List<T> getChildren();

     void setChildren(List<T> children);

     void setParent(T parent);

     T getParent();

}
