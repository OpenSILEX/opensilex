/*******************************************************************************
 *                         UserFrontConfigDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 11/04/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.front.api;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * User-specific front configuration. The menu depends on the user profiles.
 *
 * @author Valentin RIGOLLE
 */
public class UserFrontConfigDTO {

    @NotNull
    List<MenuItemDTO> menu;

    @NotNull
    boolean userIsAnonymous;

    @ApiModelProperty(value = "Application menu with routes")
    public List<MenuItemDTO> getMenu() {
        return menu;
    }

    public void setMenu(List<MenuItemDTO> menu) {
        this.menu = menu;
    }

    @ApiModelProperty(value = "User is anonymous")
    public boolean getUserIsAnonymous() {
        return userIsAnonymous;
    }

    public void setUserIsAnonymous(boolean userIsAnonymous) {
        this.userIsAnonymous = userIsAnonymous;
    }
}
