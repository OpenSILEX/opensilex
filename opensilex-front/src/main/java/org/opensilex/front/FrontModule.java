/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.front.config.FrontRoutingConfig;
import org.opensilex.front.config.Route;
import org.opensilex.front.config.MenuItem;
import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.Context;
import org.apache.catalina.valves.rewrite.RewriteValve;
import org.opensilex.OpenSilex;
import org.opensilex.config.ConfigManager;
import org.opensilex.front.api.FrontConfigDTO;
import org.opensilex.front.api.MenuItemDTO;
import org.opensilex.front.api.RouteDTO;
import org.opensilex.module.ModuleConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.rest.extensions.APIExtension;
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
    public Class<? extends ModuleConfig> getConfigClass() {
        return FrontConfig.class;
    }

    @Override
    public String getConfigId() {
        return "front";
    }

    @Override
    public void initServer(org.opensilex.server.Server server) throws Exception {
        // Register front application
        Context appContext = server.initApp("/app", "/", "/front", FrontModule.class);
        
        // Disable JAR scanner for front application because it's not required
        appContext.setJarScanner(new IgnoreJarScanner());
        
        // Add rewrite rules for application
        appContext.getPipeline().addValve(new RewriteValve());
    }

    @Override
    public void shutDownServer(org.opensilex.server.Server server) throws Exception {
        LOGGER.debug("Shutting down front app extension");
    }

    public final static String FRONT_EXTENSIONS_DIRECTORY = "front/";
    public static final String FRONT_CONFIG_FILE = "opensilex.front.yml";
    public static final String FRONT_CONFIG_PATH = FRONT_EXTENSIONS_DIRECTORY + FRONT_CONFIG_FILE;

    private FrontConfigDTO config = null;

    public FrontConfigDTO getConfigDTO() {
        if (this.config == null || OpenSilex.getInstance().isDev()) {
            FrontConfig frontConfig = getConfig(FrontConfig.class);

            FrontConfigDTO config = new FrontConfigDTO();

            config.setHomeComponent(frontConfig.homeComponent());
            config.setNotFoundComponent(frontConfig.notFoundComponent());
            config.setHeaderComponent(frontConfig.headerComponent());
            config.setLoginComponent(frontConfig.loginComponent());
            config.setMenuComponent(frontConfig.menuComponent());
            config.setFooterComponent(frontConfig.footerComponent());

            String[] themeId = frontConfig.theme().split("#");
            if (themeId.length == 2) {
                config.setThemeModule(themeId[0]);
                config.setThemeName(themeId[1]);
            }
            
            List<MenuItemDTO> globalMenu = new ArrayList<>();
            List<RouteDTO> globalRoutes = new ArrayList<>();

            for (OpenSilexModule m : OpenSilex.getInstance().getModules()) {
                try {
                    if (m.fileExists(FRONT_CONFIG_PATH)) {
                        ConfigManager cfg = new ConfigManager();
                        cfg.addSource(m.getFileInputStream(FRONT_CONFIG_PATH));
                        FrontRoutingConfig frontRoutingConfig = cfg.loadConfig("", FrontRoutingConfig.class);
                        for (MenuItem menuItem : frontRoutingConfig.menu()) {
                            MenuItemDTO menuDTO = MenuItemDTO.fromModel(menuItem);
                            globalMenu.add(menuDTO);
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

            config.setMenu(globalMenu);
            config.setRoutes(globalRoutes);

            this.config = config;
        }

        return this.config;
    }
}
