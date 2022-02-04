/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.opensilex.front.config.CustomMenuItem;
import org.opensilex.front.config.MenuItem;
import org.opensilex.front.config.MenuItemUtils;

@ApiModel
public class MenuItemDTO {

    public static MenuItemDTO fromModel(MenuItem menuItem, Map<String, String> menuLabelMap, List<String> menuExclusions, Set<String> userCredentials) {
        MenuItemDTO menuDTO = new MenuItemDTO();

        menuDTO.setId(menuItem.id());
        menuDTO.setLabel(menuItem.label());

        List<MenuItem> mc = menuItem.children();
        List<MenuItemDTO> children = new ArrayList<>(mc.size());
        for (MenuItem child : mc) {
            // Exclude menu entries based on the config and the user credentials
            if (!menuExclusions.contains(child.id()) && MenuItemUtils.hasUserCredentials(child, userCredentials)) {
                children.add(fromModel(child, menuLabelMap, menuExclusions, userCredentials));
            }
        }
        menuDTO.setChildren(children.toArray(new MenuItemDTO[children.size()]));

        RouteDTO routeDTO = RouteDTO.fromModel(menuItem.route());
        menuDTO.setRoute(routeDTO);

        menuLabelMap.put(menuDTO.getId(), menuDTO.getLabel());
        return menuDTO;
    }

    public static MenuItemDTO fromCustomModel(CustomMenuItem menuItem, Map<String, String> menuLabelMap, List<String> menuExclusions, Set<String> userCredentials, String lang) {
        MenuItemDTO menuDTO = new MenuItemDTO();

        menuDTO.setId(menuItem.id());
        Map<String, String> menuLabel = menuItem.label();
        if (menuLabel.containsKey(lang)) {
            menuDTO.setLabel(menuLabel.get(lang));
        } else if (menuLabelMap.containsKey(menuDTO.getId())) {
            menuDTO.setLabel(menuLabelMap.get(menuDTO.getId()));
        } else {
            menuDTO.setLabel(menuDTO.getId());
        }

        List<CustomMenuItem> mc = menuItem.children();
        List<MenuItemDTO> children = new ArrayList<>(mc.size());
        for (CustomMenuItem child : mc) {
            // Exclude menu entries based on the config and the user credentials
            if (!menuExclusions.contains(child.id()) && MenuItemUtils.hasUserCredentials(child, userCredentials)) {
                children.add(fromCustomModel(child, menuLabelMap, menuExclusions, userCredentials, lang));
            }
        }
        menuDTO.setChildren(children.toArray(new MenuItemDTO[children.size()]));

        RouteDTO routeDTO = RouteDTO.fromModel(menuItem.route());
        menuDTO.setRoute(routeDTO);

        return menuDTO;
    }

    @NotNull
    private String id;

    @NotNull
    private String label;

    @NotNull
    private MenuItemDTO[] children;

    private RouteDTO route;

    @ApiModelProperty(value = "Menu identifier", example = "users")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Menu label", example = "Users")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(value = "List of sub menu items")
    public MenuItemDTO[] getChildren() {
        return children;
    }

    public void setChildren(MenuItemDTO[] children) {
        this.children = children;
    }

    @ApiModelProperty(value = "Optional route definition")
    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }

}
