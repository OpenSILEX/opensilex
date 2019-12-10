/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.front.MenuItem;

public class MenuItemDTO {

    public static MenuItemDTO fromModel(MenuItem menuItem) {
        MenuItemDTO menuDTO = new MenuItemDTO();
        
        menuDTO.setId(menuItem.id());
        menuDTO.setLabel(menuItem.label());
        
        List<MenuItemDTO> children = new ArrayList<>();
        for (MenuItem child: menuItem.children()) {
            children.add(fromModel(child));
        }
        menuDTO.setChildren(children);
        
        RouteDTO routeDTO = RouteDTO.fromModel(menuItem.route());
        menuDTO.setRoute(routeDTO);
        
        return menuDTO;
    }
    
    @NotNull
    private String id;
    
    @NotNull
    private String label;

    @NotNull
    private List<MenuItemDTO> children;

    private RouteDTO route;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<MenuItemDTO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuItemDTO> children) {
        this.children = children;
    }

    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }
    
    
}
