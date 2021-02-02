/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 *
 * @author vmigot
 */
public class PartialResourceTreeDTO extends ResourceTreeDTO {

    @JsonProperty("child_count")
    protected int childCount;

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public static <T extends SPARQLTreeModel<T>> List<PartialResourceTreeDTO> fromResourceTree(SPARQLPartialTreeListModel<T> tree) {
        return fromResourceTree(tree, null);
    }

    public static <T extends SPARQLTreeModel<T>> List<PartialResourceTreeDTO> fromResourceTree(SPARQLPartialTreeListModel<T> tree, BiConsumer<T, ResourceTreeDTO> handler) {
        return fromResourceTree(tree, false, handler);
    }

    public static <T extends SPARQLTreeModel<T>> List<PartialResourceTreeDTO> fromResourceTree(SPARQLPartialTreeListModel<T> tree, boolean enableSelection) {
        return fromResourceTree(tree, enableSelection, null);
    }

    public static <T extends SPARQLTreeModel<T>> List<PartialResourceTreeDTO> fromResourceTree(SPARQLPartialTreeListModel<T> tree, boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler) {
        List<PartialResourceTreeDTO> list = new ArrayList<>(tree.getRootsCount());

        tree.listRoots(root -> {
            PartialResourceTreeDTO rootDto = fromResourceTreeRecursive(root, tree, enableSelection, handler);
            list.add(rootDto);
        });

        return list;
    }

    private static <T extends SPARQLTreeModel<T>> PartialResourceTreeDTO fromResourceTreeRecursive(T model, SPARQLPartialTreeListModel<T> tree, boolean enableSelection, BiConsumer<T, ResourceTreeDTO> handler) {
        PartialResourceTreeDTO dto = new PartialResourceTreeDTO();
        dto.fromModel(model);

        dto.setSelected(enableSelection && tree.isSelected(model));

        List<ResourceTreeDTO> childrenDTOs = new ArrayList<>(tree.getChildCount(model));
        tree.listChildren(model, child -> {
            ResourceTreeDTO childDTO = fromResourceTreeRecursive(child, tree, enableSelection, handler);
            childDTO.setParent(model.getUri());
            childrenDTOs.add(childDTO);
        });
        dto.setChildren(childrenDTOs);
        dto.setChildCount(tree.getTotalChildCount(model));

        if (handler != null) {
            handler.accept(model, dto);
        }

        return dto;
    }
}
