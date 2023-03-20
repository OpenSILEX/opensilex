/*******************************************************************************
 *                         UserConfigService.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 12/01/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.front.config;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.front.api.MenuItemDTO;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;

/**
 * This service builds User-specific configuration for the module (such as the menu configuration).
 *
 * @author Valentin RIGOLLE
 */
public class UserConfigService {

    private final SPARQLService sparql;

    private Map<URI, List<MenuItemDTO>> userMenuCache = new HashMap<>();

    public UserConfigService(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public List<MenuItemDTO> getUserMenu(AccountModel user, List<MenuItem> menu, Map<String, String> menuLabelMap, List<String> menuExclusions, List<CustomMenuItem> customMenu) {
        buildUserMenu(user, menu, menuLabelMap, menuExclusions, customMenu);
        return userMenuCache.get(user.getUri());
    }

    protected void buildUserMenu(AccountModel user, List<MenuItem> menu, Map<String, String> menuLabelMap, List<String> menuExclusions, List<CustomMenuItem> customMenu) {
        if (false && userMenuCache.containsKey(user.getUri())) {
            return;
        }

        AccountDAO accountDAO = new AccountDAO(sparql);
        Set<String> userCredentials;
        if (user.isAdmin()) {
            userCredentials = null;
        } else {
            try {
                userCredentials = new HashSet<>(accountDAO.getCredentialList(user.getUri()));
            } catch (Exception ignored) {
                userCredentials = Collections.emptySet();
            }
        }

        List<MenuItemDTO> userMenu = new ArrayList<>();

        if (CollectionUtils.isEmpty(customMenu)) {
            for (MenuItem menuItem : menu) {
                // Exclude menu entries based on the config and the user credentials
                if (!menuExclusions.contains(menuItem.id()) && MenuItemUtils.hasUserCredentials(menuItem, userCredentials)) {
                    MenuItemDTO menuDTO = MenuItemDTO.fromModel(menuItem, menuLabelMap, menuExclusions, userCredentials);
                    if ((menuDTO.getRoute() != null && !StringUtils.isEmpty(menuDTO.getRoute().getPath()))
                            || !ArrayUtils.isEmpty(menuDTO.getChildren())) {
                        userMenu.add(menuDTO);
                    }
                }
            }
        } else {
            for (CustomMenuItem customMenuItem : customMenu) {
                // Exclude menu entries based on the config and the user credentials
                if (!menuExclusions.contains(customMenuItem.id()) && MenuItemUtils.hasUserCredentials(customMenuItem, userCredentials)) {
                    MenuItemDTO menuDTO = MenuItemDTO.fromCustomModel(customMenuItem, menuLabelMap, menuExclusions, userCredentials, user.getLanguage());
                    if ((menuDTO.getRoute() != null && !StringUtils.isEmpty(menuDTO.getRoute().getPath()))
                            || !ArrayUtils.isEmpty(menuDTO.getChildren())) {
                        userMenu.add(menuDTO);
                    }
                }
            }
        }

        userMenuCache.put(user.getUri(), userMenu);
    }
}
