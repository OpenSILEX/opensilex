/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.front.config.Route;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class RouteDTO {

    public static RouteDTO fromModel(Route route) {
        if (route.path().isEmpty() || route.component().isEmpty()) {
            return null;
        }

        RouteDTO routeDTO = new RouteDTO();

        routeDTO.setPath(route.path());
        routeDTO.setComponent(route.component());
        routeDTO.setCredentials(route.credentials());

        return routeDTO;
    }

    @NotNull
    private String path;

    @NotNull
    private String component;

    private List<String> credentials;

    @ApiModelProperty(value = "Route path", example = "/users")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @ApiModelProperty(value = "Route component", example = "opensilex.UserList")
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @ApiModelProperty(value = "Required credentials list for this route")
    public List<String> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<String> credentials) {
        this.credentials = credentials;
    }
}
