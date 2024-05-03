/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vince
 */
public class ResourceTreeDTO extends NamedResourceDTO<SPARQLTreeModel<?>> {

    private URI parent;

    private boolean selected;

    private boolean disabled;

    private List<ResourceTreeDTO> children = new LinkedList<>();


    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public List<ResourceTreeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<ResourceTreeDTO> children) {
        this.children = children;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    //Some utility recursive methods
    /**
     * Returns true if every node verifies the predicate
     * @param predicate, the predicate to apply to every node
     */
    public boolean allMatch(Predicate<ResourceTreeDTO> predicate) {
        if (!predicate.test(this)) {
            return false;
        }
        for (var child : this.children) {
            if (!child.allMatch(predicate)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Visits every node of tree and applies consumer
     * @param consumer to apply
     * @param includeThis sais if consumer should be applied to root node
     */
    public void visit(Consumer<ResourceTreeDTO> consumer, boolean includeThis){
        if(includeThis){
            consumer.accept(this);
        }
        if (getChildren() != null) {
            getChildren().forEach(
                    child -> child.visit(consumer, true)
            );
        }
    }

    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(List<SPARQLTreeListModel<?>>trees) {
        List<ResourceTreeDTO> dtoList = new LinkedList<>();
        trees.forEach(tree ->  ResourceTreeDTO.addTreeToList(dtoList,tree,false,null));
        return dtoList;

    }

    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(SPARQLTreeListModel<T>tree) {
        return fromResourceTree(false,null, Collections.singletonList(tree));
    }


    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(boolean enableSelection, SPARQLTreeListModel<T> tree) {
        return fromResourceTree(enableSelection,null, Collections.singletonList(tree));
    }

    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler, Collection<SPARQLTreeListModel<T>> trees) {
        List<ResourceTreeDTO> list = new ArrayList<>();

        for (SPARQLTreeListModel<T> tree : trees) {
            addTreeToList(list,tree,enableSelection,handler);
        }

        return list;
    }

    public static <T extends SPARQLTreeModel<T>> void addTreeToList(List<ResourceTreeDTO> list, SPARQLTreeListModel<T> tree, boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler) {
        tree.listRoots(root -> {
            ResourceTreeDTO rootDto = fromResourceTreeRecursive(root, tree, enableSelection, handler);
            list.add(rootDto);
        });
    }



    private static <T extends SPARQLTreeModel<T>> ResourceTreeDTO fromResourceTreeRecursive(T model, SPARQLTreeListModel<T> tree, boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler) {
        ResourceTreeDTO dto = new ResourceTreeDTO();
        dto.fromModel(model);

        dto.setSelected(enableSelection && tree.isSelected(model));

        List<ResourceTreeDTO> childrenDTOs = new ArrayList<>(tree.getChildCount(model));
        tree.listChildren(model, child -> {
            ResourceTreeDTO childDTO = fromResourceTreeRecursive(child, tree, enableSelection, handler);
            childDTO.setParent(model.getUri());
            childrenDTOs.add(childDTO);
        });
        dto.setChildren(childrenDTOs);

        if (handler != null) {
            handler.accept(model, dto);
        }

        return dto;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceTreeDTO other = (ResourceTreeDTO) obj;
        return Objects.equals(this.uri, other.uri);
    }

}
