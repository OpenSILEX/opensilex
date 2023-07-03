package org.opensilex.sparql.model;

import java.util.List;

/**
 * The model of a node of a <b>directed acyclic graph (DAG)</b>. The node has references to its parents and
 * its children.
 *
 * @author Valentin RIGOLLE
 */
public abstract class SPARQLDagModel<T extends SPARQLDagModel<T>> extends SPARQLNamedResourceModel<T> {
    public static final String PARENTS_FIELD = "parents";

    public static final String CHILDREN_FIELD = "children";

    public abstract List<T> getParents();

    public abstract void setParents(List<T> parents);

    public abstract List<T> getChildren();

    public abstract void setChildren(List<T> children);
}
