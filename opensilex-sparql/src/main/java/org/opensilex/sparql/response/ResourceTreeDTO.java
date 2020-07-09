/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vince
 */
public class ResourceTreeDTO extends NamedResourceDTO {

    private URI parent;

    private boolean selected;
    
    private boolean disabled;

    private List<ResourceTreeDTO> children;

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
    
    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(SPARQLTreeListModel<T> tree) {
        return fromResourceTree(tree, null);
    }

    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(SPARQLTreeListModel<T> tree, BiConsumer<T, ResourceTreeDTO> handler) {
        return fromResourceTree(tree, false, handler);
    }

    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(SPARQLTreeListModel<T> tree, boolean enableSelection) {
        return fromResourceTree(tree, enableSelection, null);
    }

    public static <T extends SPARQLTreeModel<T>> List<ResourceTreeDTO> fromResourceTree(SPARQLTreeListModel<T> tree, boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler) {
        List<ResourceTreeDTO> list = new ArrayList<>(tree.getRootsCount());

        tree.listRoots(root -> {
            ResourceTreeDTO rootDto = fromResourceTreeRecursive(root, tree, enableSelection, handler);
            list.add(rootDto);
        });

        return list;
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
