/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.apache.catalina.Context;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.config.ConfigManager;
import org.opensilex.core.CoreConfig;
import org.opensilex.core.CoreModule;
import org.opensilex.front.api.*;
import org.opensilex.front.config.*;
import org.opensilex.security.*;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.email.EmailService;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.extensions.ServerExtension;
import org.opensilex.server.scanner.IgnoreJarScanner;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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

    private UserConfigService userConfigService;

    private FrontConfigDTO config = null;
    private List<MenuItem> globalMenu = null;
    private URI lastUserUri = null;

    public FrontConfigDTO getConfigDTO(AccountModel currentUser, SPARQLService sparql) {
        FrontConfig frontConfig = getConfig(FrontConfig.class);

        // General config, valid for all users
        if (this.config == null || currentUser.getUri() != lastUserUri || getOpenSilex().isDev()) {
            String lang = currentUser.getLanguage();
            lastUserUri = currentUser.getUri();

            FrontConfigDTO config = new FrontConfigDTO();

            config.setPathPrefix(getApplicationPathPrefix());
            config.setHomeComponent(frontConfig.homeComponent());
            config.setNotFoundComponent(frontConfig.notFoundComponent());
            config.setHeaderComponent(frontConfig.headerComponent());
            config.setLoginComponent(frontConfig.loginComponent());
            config.setMenuComponent(frontConfig.menuComponent());
            config.setFooterComponent(frontConfig.footerComponent());
            config.setGeocodingService(frontConfig.geocodingService());
            config.setMenuExclusions(frontConfig.menuExclusions());
            config.setApplicationName(frontConfig.applicationName());
            config.setConnectAsGuest(frontConfig.connectAsGuest());

            DashboardConfigDTO dashboard = new DashboardConfigDTO();
            try {
                dashboard.setShowMetrics(getOpenSilex().getModuleConfig(CoreModule.class, CoreConfig.class).metrics().enableMetrics());
                GraphConfigDTO graph1 = new GraphConfigDTO();
                graph1.setVariable(new URI(frontConfig.dashboard().graph1().variable()));
                dashboard.setGraph1(graph1);
                GraphConfigDTO graph2 = new GraphConfigDTO();
                graph2.setVariable(new URI(frontConfig.dashboard().graph2().variable()));
                dashboard.setGraph2(graph2);
                GraphConfigDTO graph3 = new GraphConfigDTO();
                graph3.setVariable(new URI(frontConfig.dashboard().graph3().variable()));
                dashboard.setGraph3(graph3);
            } catch (URISyntaxException | OpenSilexModuleNotFoundException ignored) {
            }
            config.setDashboard(dashboard);

            try {
                config.setVersionLabel(VersionLabel.valueOf(frontConfig.versionLabel().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }

            AuthenticationService auth = getOpenSilex().getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
            // OpenID configuration
            try {
                URI authURI = auth.getOpenIDAuthenticationURI();
                if (authURI != null) {
                    config.setOpenIDAuthenticationURI(authURI.toString());
                    OpenIDConfig openid = getOpenSilex().getModuleConfig(SecurityModule.class, SecurityConfig.class).openID();
                    String connectionTitle = openid.connectionTitle().get(lang);
                    config.setOpenIDConnectionTitle(connectionTitle);
                }
            } catch (Exception ex) {
                LOGGER.error("Unexpected error", ex);
            }

            // SAML configuration
            try {
                URI samlProxyLoginURI = auth.getSamlProxyLoginURI();
                if (Objects.nonNull(samlProxyLoginURI)) {
                    config.setSamlProxyLoginURI(samlProxyLoginURI.toString());
                    SAMLConfig samlConfig = getOpenSilex()
                            .getModuleConfig(SecurityModule.class, SecurityConfig.class)
                            .saml();
                    String samlConnectionTitle = samlConfig.connectionTitle().get(lang);
                    config.setSamlConnectionTitle(samlConnectionTitle);
                }
            } catch (OpenSilexModuleNotFoundException e) {
                LOGGER.error("Security module not found", e);
                throw new RuntimeException(e);
            }

            try {
                EmailService emailService = getOpenSilex().getModuleConfig(SecurityModule.class, SecurityConfig.class).email();
                EmailConfig emailConfig = (EmailConfig) emailService.getConfig();
                config.setActivateResetPassword(emailConfig.enable());
            } catch (OpenSilexModuleNotFoundException ex) {
                LOGGER.error("Unexpected error", ex);
            }

            String[] themeId = frontConfig.theme().split("#");
            if (themeId.length == 2) {
                config.setThemeModule(themeId[0]);
                config.setThemeName(themeId[1]);
            }

            List<RouteDTO> globalRoutes = new ArrayList<>();
            globalMenu = new ArrayList<>();

            for (OpenSilexModule m : getOpenSilex().getModules()) {
                try {
                    if (m.fileExists(FRONT_CONFIG_PATH)) {
                        ConfigManager cfg = new ConfigManager();
                        cfg.addSource(m.getFileInputStream(FRONT_CONFIG_PATH));
                        FrontRoutingConfig frontRoutingConfig = cfg.loadConfig("", FrontRoutingConfig.class);
                        globalMenu.addAll(frontRoutingConfig.menu());

                        for (Route route : frontRoutingConfig.routes()) {
                            RouteDTO routeDTO = RouteDTO.fromModel(route);
                            globalRoutes.add(routeDTO);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Error will loading front configuration opensilex.front.yml for: " + m.getClass().getCanonicalName(), ex);
                }
            }

            config.setRoutes(globalRoutes);

            this.config = config;
        }

        // User-specific config
        if (userConfigService == null) {
            userConfigService = new UserConfigService(sparql);
        }

        return this.config;
    }

    public UserFrontConfigDTO getUserConfigDTO(AccountModel currentUser) {
        FrontConfig frontConfig = getConfig(FrontConfig.class);
        UserFrontConfigDTO userConfig = new UserFrontConfigDTO();

        userConfig.setMenu(userConfigService.getUserMenu(currentUser, globalMenu, new HashMap<>(),
                frontConfig.menuExclusions(), frontConfig.customMenu()));

        userConfig.setUserIsAnonymous(currentUser.isAnonymous());

        return userConfig;
    }

    @Override
    public void install(boolean reset) throws Exception {
        FrontConfig frontConfig = getConfig(FrontConfig.class);
        if (frontConfig.connectAsGuest()) {
          getOpenSilex().getModuleByClass(SecurityModule.class).createDefaultGuestGroupUserProfile();
        }
    }
}