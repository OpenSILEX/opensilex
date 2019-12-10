/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.front.Route;

/**
 *
 * @author vidalmor
 */
public class RouteDTO {

    public static RouteDTO fromModel(Route route) {
        if (route.path().isEmpty() || route.component().isEmpty()) {
            return null;
        }

        RouteDTO routeDTO = new RouteDTO();

        routeDTO.setPath(route.path());
        routeDTO.setComponent(route.component());
        routeDTO.setAccess(route.access());
        
        return routeDTO;
    }

    @NotNull
    private String path;

    @NotNull
    private String component;

    private List<String> access;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public List<String> getAccess() {
        return access;
    }

    public void setAccess(List<String> access) {
        this.access = access;
    }
}
