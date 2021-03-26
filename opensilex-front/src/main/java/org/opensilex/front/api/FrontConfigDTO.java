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

@ApiModel
public class FrontConfigDTO {

    @NotNull
    String pathPrefix;

    @NotNull
    String homeComponent;

    @NotNull
    String notFoundComponent;

    @NotNull
    String headerComponent;

    @NotNull
    String loginComponent;

    @NotNull
    String menuComponent;

    @NotNull
    String footerComponent;

    @NotNull
    List<MenuItemDTO> menu;

    @NotNull
    List<RouteDTO> routes;

    String themeModule;

    String themeName;

    String openIDAuthenticationURI;

    String openIDConnectionTitle;

    @ApiModelProperty(value = "Application url path prefix", example = "app")
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @ApiModelProperty(value = "Home component identifier", example = "opensilex-DefaultHomeComponent")
    public String getHomeComponent() {
        return homeComponent;
    }

    public void setHomeComponent(String homeComponent) {
        this.homeComponent = homeComponent;
    }

    @ApiModelProperty(value = "Not found component identifier", example = "opensilex-DefaultNotFoundComponent")
    public String getNotFoundComponent() {
        return notFoundComponent;
    }

    public void setNotFoundComponent(String notFoundComponent) {
        this.notFoundComponent = notFoundComponent;
    }

    @ApiModelProperty(value = "Menu component identifier", example = "opensilex-DefaultMenuComponent")
    public String getMenuComponent() {
        return menuComponent;
    }

    public void setMenuComponent(String menuComponent) {
        this.menuComponent = menuComponent;
    }

    @ApiModelProperty(value = "Footer component identifier", example = "opensilex-DefaultFooterComponent")
    public String getFooterComponent() {
        return footerComponent;
    }

    public void setFooterComponent(String footerComponent) {
        this.footerComponent = footerComponent;
    }

    @ApiModelProperty(value = "Header component identifier", example = "opensilex-DefaultHeaderComponent")
    public String getHeaderComponent() {
        return headerComponent;
    }

    public void setHeaderComponent(String headerComponent) {
        this.headerComponent = headerComponent;
    }

    @ApiModelProperty(value = "Login component identifier", example = "opensilex-DefaultLoginComponent")
    public String getLoginComponent() {
        return loginComponent;
    }

    public void setLoginComponent(String loginComponent) {
        this.loginComponent = loginComponent;
    }

    @ApiModelProperty(value = "Application menu with routes")
    public List<MenuItemDTO> getMenu() {
        return menu;
    }

    public void setMenu(List<MenuItemDTO> menu) {
        this.menu = menu;
    }

    @ApiModelProperty(value = "List of configured routes")
    public List<RouteDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDTO> routes) {
        this.routes = routes;
    }

    @ApiModelProperty(value = "Theme module identifier")
    public String getThemeModule() {
        return themeModule;
    }

    public void setThemeModule(String themeModule) {
        this.themeModule = themeModule;
    }

    @ApiModelProperty(value = "Theme module name")
    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    @ApiModelProperty(value = "OpenID Authorization URI")
    public String getOpenIDAuthenticationURI() {
        return openIDAuthenticationURI;
    }

    public void setOpenIDAuthenticationURI(String openIDAuthorizationURI) {
        this.openIDAuthenticationURI = openIDAuthorizationURI;
    }

    public String getOpenIDConnectionTitle() {
        return openIDConnectionTitle;
    }

    public void setOpenIDConnectionTitle(String openIDConnectionTitle) {
        this.openIDConnectionTitle = openIDConnectionTitle;
    }

}
