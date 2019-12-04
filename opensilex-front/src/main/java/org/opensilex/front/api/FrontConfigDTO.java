/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

public class FrontConfigDTO {

    String homeComponent;
    
    String notFoundComponent;
    
    String headerComponent;

    String loginComponent;

    String menuComponent;
    
    String footerComponent;

    public String getHomeComponent() {
        return homeComponent;
    }

    public void setHomeComponent(String homeComponent) {
        this.homeComponent = homeComponent;
    }

    public String getNotFoundComponent() {
        return notFoundComponent;
    }

    public void setNotFoundComponent(String notFoundComponent) {
        this.notFoundComponent = notFoundComponent;
    }

    public String getMenuComponent() {
        return menuComponent;
    }

    public void setMenuComponent(String menuComponent) {
        this.menuComponent = menuComponent;
    }

    public String getFooterComponent() {
        return footerComponent;
    }

    public void setFooterComponent(String footerComponent) {
        this.footerComponent = footerComponent;
    }

    public String getHeaderComponent() {
        return headerComponent;
    }

    public void setHeaderComponent(String headerComponent) {
        this.headerComponent = headerComponent;
    }

    public String getLoginComponent() {
        return loginComponent;
    }

    public void setLoginComponent(String loginComponent) {
        this.loginComponent = loginComponent;
    }

}
