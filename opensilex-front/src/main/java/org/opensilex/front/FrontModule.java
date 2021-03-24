/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import org.opensilex.front.config.FrontRoutingConfig;
import org.opensilex.front.config.Route;
import org.opensilex.front.config.MenuItem;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.catalina.Context;
import org.opensilex.config.ConfigManager;
import org.opensilex.front.api.FrontConfigDTO;
import org.opensilex.front.api.MenuItemDTO;
import org.opensilex.front.api.RouteDTO;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.front.config.CustomMenuItem;
import org.opensilex.security.OpenIDConfig;
import org.opensilex.security.SecurityConfig;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.extensions.ServerExtension;
import org.opensilex.server.scanner.IgnoreJarScanner;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class FrontModule extends OpenSilexModule implements ServerExtension, APIExtension {

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FrontModule.class);

    @Override
    public Class<?> getConfigClass() {
        return FrontConfig.class;
    }

    @Override
    public String getConfigId() {
        return "front";
    }

    public String getApplicationPathPrefix() {
        try {
            ServerConfig cfg = getOpenSilex().getModuleConfig(ServerModule.class, ServerConfig.class);
            String pathPrefix = cfg.pathPrefix();
            return pathPrefix;
        } catch (OpenSilexModuleNotFoundException ex) {
            return "";
        }
    }

    @Override
    public void initServer(org.opensilex.server.Server server) throws Exception {
        // Register front application
        String pathPrefix = getApplicationPathPrefix();
        Context appContext = server.initApp(pathPrefix + "/app", "/", "/front", FrontModule.class);

        // Disable JAR scanner for front application because it's not required
        appContext.setJarScanner(new IgnoreJarScanner());

        // Add rewrite rules for application
        FrontRewriteValve valve = new FrontRewriteValve();
        appContext.getPipeline().addValve(valve);
        valve.initRules();
    }

    @Override
    public void shutDownServer(org.opensilex.server.Server server) throws Exception {
        LOGGER.debug("Shutting down front app extension");
    }

    public final static String FRONT_EXTENSIONS_DIRECTORY = "front/";
    public static final String FRONT_CONFIG_FILE = "opensilex.front.yml";
    public static final String FRONT_CONFIG_PATH = FRONT_EXTENSIONS_DIRECTORY + FRONT_CONFIG_FILE;

    private FrontConfigDTO config = null;

    public FrontConfigDTO getConfigDTO(String lang) {
        if (this.config == null || getOpenSilex().isDev()) {
            FrontConfig frontConfig = getConfig(FrontConfig.class);

            FrontConfigDTO config = new FrontConfigDTO();

            config.setPathPrefix(getApplicationPathPrefix());
            config.setHomeComponent(frontConfig.homeComponent());
            config.setNotFoundComponent(frontConfig.notFoundComponent());
            config.setHeaderComponent(frontConfig.headerComponent());
            config.setLoginComponent(frontConfig.loginComponent());
            config.setMenuComponent(frontConfig.menuComponent());
            config.setFooterComponent(frontConfig.footerComponent());

            try {
                AuthenticationService auth = getOpenSilex().getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
                URI authURI = auth.getOpenIDAuthenticationURI();
                if (authURI != null) {
                    config.setOpenIDAuthenticationURI(authURI.toString());
                    OpenIDConfig openid =  getOpenSilex().getModuleConfig(SecurityModule.class, SecurityConfig.class).openID();
                    String connectionTitle = openid.connectionTitle().get(lang);
                    if (connectionTitle == null || connectionTitle.isEmpty()) {
                        connectionTitle = "Connect with OpenID";
                    }
                    config.setOpenIDConnectionTitle(connectionTitle);
                }
            } catch (Exception ex) {
                LOGGER.error("Unexpected error", ex);
            }

            String[] themeId = frontConfig.theme().split("#");
            if (themeId.length == 2) {
                config.setThemeModule(themeId[0]);
                config.setThemeName(themeId[1]);
            }

            Map<String, String> menuLabelMap = new HashMap<>();
            List<MenuItemDTO> globalMenu = new ArrayList<>();
            List<RouteDTO> globalRoutes = new ArrayList<>();
            List<String> menuExclusions = frontConfig.menuExclusions();

            for (OpenSilexModule m : getOpenSilex().getModules()) {
                try {
                    if (m.fileExists(FRONT_CONFIG_PATH)) {
                        ConfigManager cfg = new ConfigManager();
                        cfg.addSource(m.getFileInputStream(FRONT_CONFIG_PATH));
                        FrontRoutingConfig frontRoutingConfig = cfg.loadConfig("", FrontRoutingConfig.class);
                        for (MenuItem menuItem : frontRoutingConfig.menu()) {
                            if (!menuExclusions.contains(menuItem.id())) {
                                MenuItemDTO menuDTO = MenuItemDTO.fromModel(menuItem, menuLabelMap, menuExclusions);
                                globalMenu.add(menuDTO);
                            }
                        }

                        for (Route route : frontRoutingConfig.routes()) {
                            RouteDTO routeDTO = RouteDTO.fromModel(route);
                            globalRoutes.add(routeDTO);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Error will loading front configuration opensilex.front.yml for: " + m.getClass().getCanonicalName(), ex);
                }
            }

            List<CustomMenuItem> customMenu = frontConfig.customMenu();
            if (customMenu != null && customMenu.size() > 0) {
                List<MenuItemDTO> customMenuDTO = new ArrayList<>();
                for (CustomMenuItem menuItem : customMenu) {
                    if (!menuExclusions.contains(menuItem.id())) {
                        MenuItemDTO menuDTO = MenuItemDTO.fromCustomModel(menuItem, menuLabelMap, menuExclusions, lang);
                        customMenuDTO.add(menuDTO);
                    }
                }
                globalMenu = customMenuDTO;
            }

            config.setMenu(globalMenu);
            config.setRoutes(globalRoutes);

            this.config = config;
        }

        return this.config;
    }
}
