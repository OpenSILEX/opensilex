/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author vince
 */
public abstract class SPARQLTreeModel<T extends SPARQLTreeModel<T>> extends SPARQLNamedResourceModel<SPARQLTreeModel<T>> {

    protected T parent;
    public static final String PARENT_FIELD = "parent";

    protected List<T> children = new ArrayList<>();
    public static final String CHILDREN_FIELD = "children";

    public T getParent() {
        return parent;
    }

    public void setParent(T parent) {
        this.parent = parent;
    }

    public List<T> getChildren() {
        return children;
    }

    public void setChildren(List<T> children) {
        this.children = children;
    }

    public List<T> getNodes() {
        List<T> visitedList = new ArrayList<>();
        visit(visitedList::add);
        return visitedList;
    }

    public void visit(Consumer<T> consumer){
        consumer.accept((T) this);
        if (getChildren() != null) {
            getChildren().forEach(
                    child -> child.visit(consumer)
            );
        }
    }


}
