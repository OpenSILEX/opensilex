//******************************************************************************
//                          DependencyManager.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dependencies;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Dependency manager based on Maven Resolver API for modules dependency resolution
 * see: https://maven.apache.org/resolver/index.html
 * </pre>
 *
 * @author Vincent Migot
 */
public class DependencyManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(DependencyManager.class);
    
    private final static String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2/";

    /**
     * Generate a unique key for an artifact
     *
     * @param artifact Artifact used to generate the key
     * @return Unique key for the given artifact
     */
    private static String getArtifactKey(Artifact artifact) {
        String key = artifact.getGroupId()
                + ":" + artifact.getArtifactId()
                + ":" + artifact.getProperty("packaging", "jar")
                + ":" + artifact.getVersion();
        return key;
    }

    /**
     * Get Maven local system repository
     *
     * @return Maven local system repository
     */
    private static RepositorySystem getRepositorySystem() {
        DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator
                .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
        serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        serviceLocator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                LOGGER.error("Error creating service", exception);
            }
        });

        return serviceLocator.getService(RepositorySystem.class);
    }

    /**
     * Get session for Maven local system repository
     *
     * @return Session for Maven local system repository
     */
    private static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system, String repositoryPath) {
        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
                .newSession();

        LocalRepository localRepository = new LocalRepository(repositoryPath);
        repositorySystemSession.setLocalRepositoryManager(
                system.newLocalRepositoryManager(repositorySystemSession, localRepository));

        repositorySystemSession.setRepositoryListener(new DependencyLogger());

        return repositorySystemSession;
    }

    /**
     * Get Maven remote repository (Maven central)
     *
     * @return Maven remote repository
     */
    private static RemoteRepository getCentralMavenRepository() {
        return new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL_URL).build();
    }

    /**
     * Get Maven remote repositories defined in pom file
     *
     * @param model the pom model to check
     * @return list of remote repositories
     */
    private static List<RemoteRepository> getPomRemoteRepositories(Model model) {
        List<RemoteRepository> remoteRepoList = new ArrayList<>();
        remoteRepoList.add(getCentralMavenRepository());

        model.getRepositories().forEach((repo) -> {
            remoteRepoList.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
        });

        return remoteRepoList;
    }

    /**
     * Local system repository
     */
    private RepositorySystem system;

    /**
     * Session for local system repository
     */
    private RepositorySystemSession session;

    /**
     * Local system repository path
     */
    private String repositoryPath = System.getProperty("user.home") + "/.m2/repository/";

    /**
     * Loaded dependencies list
     */
    private final List<String> loadedDependencies = new ArrayList<>();

    /**
     * Build-in dependencies list
     */
    private final List<String> buildinDependencies = new ArrayList<>();

    /**
     * Constructor for dependency manager based on a main pom file
     *
     * @param mainPom Base pom file
     * @throws ModelBuildingException In case of bad pom files
     * @throws DependencyResolutionException Dependency resolution issue
     * @throws MalformedURLException In case of bad url format
     */
    public DependencyManager(File mainPom) throws ModelBuildingException, DependencyResolutionException, MalformedURLException {
        initRegistries();
        loadDependencies(mainPom, false);
        buildinDependencies.addAll(loadedDependencies);
    }

    /**
     * Init local repository access
     */
    private void initRegistries() {
        system = getRepositorySystem();
        session = getRepositorySystemSession(system, repositoryPath);
    }

    /**
     * Register pom.xml file as a loaded dependency
     *
     * @param pom pom.xml to register
     * @return pom.xml parsed model
     * @throws ModelBuildingException Bad pom.xml format
     */
    private Model registerPom(File pom) throws ModelBuildingException {
        Model model = buildPomModel(pom);

        loadedDependencies.add(model.getGroupId()
                + ":" + model.getArtifactId()
                + ":" + model.getPackaging()
                + ":" + model.getVersion());

        return model;
    }

    /**
     * Load pom.xml dependencies and try download them if needed if flag
     * downloadWithMaven is true.
     *
     * @param pom pom.xml to get dependencies
     * @param downloadWithMaven Flag to determine if missing dependencies should
     * be downloaded
     * @return List of downloaded dependencies
     * @throws ModelBuildingException In case of bad pom files
     * @throws DependencyResolutionException Dependency resolution issue
     * @throws MalformedURLException In case of bad url format
     */
    private List<URL> loadDependencies(File pom, boolean downloadWithMaven) throws DependencyResolutionException, ModelBuildingException, MalformedURLException {
        List<URL> resolvedDependencies = new ArrayList<>();

        // Parse pom.xml to a Model
        Model model = registerPom(pom);
        LOGGER.debug(String.format("Maven model resolved: %s, parsing its dependencies...", model));

        // For each dependencies
        for (org.apache.maven.model.Dependency d : model.getDependencies()) {
            if (!d.getType().equals("war")) {

                // Create artifact from dependency
                Artifact artifact = new DefaultArtifact(
                        d.getGroupId(),
                        d.getArtifactId(),
                        d.getClassifier(),
                        d.getType(),
                        d.getVersion()
                );
                String key = getArtifactKey(artifact);

                LOGGER.debug(String.format("Loading dependency: %s", key));
                // If dependency is not already loaded
                if (!loadedDependencies.contains(key)) {
                    // Add it to the loaded list
                    loadedDependencies.add(key);

                    // Downlaod it if needed woth it's own dependencies
                    if (downloadWithMaven) {
                        CollectRequest collectRequest = new CollectRequest(
                                new Dependency(artifact, JavaScopes.COMPILE),
                                getPomRemoteRepositories(model)
                        );

                        // Get artifact dependencies in compile scope
                        DependencyFilter filterScope = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
                        // Remove build-in dependencies from scope
                        DependencyFilter filterPattern = new PatternExclusionsDependencyFilter(buildinDependencies);
                        // Create dependency request
                        DependencyRequest request = new DependencyRequest(collectRequest, DependencyFilterUtils.orFilter(filterScope, filterPattern));
                        // Get all matching dependencies
                        DependencyResult results = system.resolveDependencies(session, request);
                        // Download all results and add the to loaded dependencies list
                        for (ArtifactResult result : results.getArtifactResults()) {
                            if (result.isResolved()) {
                                Artifact resultArtifact = result.getArtifact();
                                loadedDependencies.add(getArtifactKey(resultArtifact));
                                resolvedDependencies.add(resultArtifact.getFile().toURI().toURL());
                            }
                        }
                    }
                }
            }
        }

        return resolvedDependencies;
    }

    /**
     * Load all modules dependencies in list
     *
     * @param jarModulesURLs List of modules JAR
     * @return List of dependencies URL
     * @throws IOException In case of file access issues
     * @throws ModelBuildingException In case of bad pom files
     */
    public List<URL> loadModulesDependencies(List<URL> jarModulesURLs) throws IOException, DependencyResolutionException, ModelBuildingException {
        List<URL> dependenciesUrl = new ArrayList<>();

        // Register all pom files
        for (URL jarURL : jarModulesURLs) {
            File pom = ClassUtils.getPomFile(ClassUtils.getJarFileFromURL(jarURL));
            registerPom(pom);
        }

        // Load all dependencies
        for (URL jarURL : jarModulesURLs) {
            File jarFile = ClassUtils.getJarFileFromURL(jarURL);
            dependenciesUrl.addAll(loadDependencies(ClassUtils.getPomFile(jarFile), true));
        }

        // Return dependencies aggregated list
        return dependenciesUrl;
    }

    /**
     * Build pom.xml model from pom.xml file
     *
     * @param pom pom.xml to analyse
     * @return pom.xml model
     * @throws ModelBuildingException In case of bad pom files
     */
    private Model buildPomModel(File pom) throws ModelBuildingException {
        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setPomFile(pom);

        ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult modelBuildingResult = modelBuilder.build(modelBuildingRequest);

        Model model = modelBuildingResult.getEffectiveModel();

        return model;
    }
}
