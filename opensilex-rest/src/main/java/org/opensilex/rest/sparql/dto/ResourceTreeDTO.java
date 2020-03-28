/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.sparql.dto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.tree.ResourceTree;

/**
 *
 * @author vince
 */
public class ResourceTreeDTO extends NamedResourceGetDTO {

    private URI parent;

    private boolean selected;

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

    public static <T extends SPARQLTreeModel> List<ResourceTreeDTO> fromResourceTree(ResourceTree<T> tree) {
        return fromResourceTree(tree, null);
    }

    public static <T extends SPARQLTreeModel> List<ResourceTreeDTO> fromResourceTree(ResourceTree<T> tree, BiConsumer<T, ResourceTreeDTO> handler) {
        return fromResourceTree(tree, false, handler);
    }

    public static <T extends SPARQLTreeModel> List<ResourceTreeDTO> fromResourceTree(ResourceTree<T> tree, boolean enableSelection) {
        return fromResourceTree(tree, enableSelection, null);
    }

    public static <T extends SPARQLTreeModel> List<ResourceTreeDTO> fromResourceTree(ResourceTree<T> tree, boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler) {
        List<ResourceTreeDTO> list = new ArrayList<>();

        tree.listRoots(root -> {
            ResourceTreeDTO rootDto = fromResourceTreeRecursive(root, tree, enableSelection);
            list.add(rootDto);
            if (handler != null) {
                handler.accept(root, rootDto);
            }
        });

        return list;
    }

    private static <T extends SPARQLTreeModel> ResourceTreeDTO fromResourceTreeRecursive(T model, ResourceTree<T> tree, boolean enableSelection) {
        ResourceTreeDTO dto = new ResourceTreeDTO();

        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setName(model.getName());
        dto.setSelected(enableSelection && tree.isSelected(model));

        List<ResourceTreeDTO> childrenDTOs = new ArrayList<>();
        tree.listChildren(model, child -> {
            ResourceTreeDTO childDTO = fromResourceTreeRecursive(child, tree, enableSelection);
            childDTO.setParent(model.getUri());
            childrenDTOs.add(childDTO);
        });
        dto.setChildren(childrenDTOs);

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
