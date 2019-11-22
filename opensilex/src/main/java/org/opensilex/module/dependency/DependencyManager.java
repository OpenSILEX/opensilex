//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.dependency;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.maven.model.*;
import org.apache.maven.model.building.*;
import org.apache.maven.repository.internal.*;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.*;
import org.eclipse.aether.collection.*;
import org.eclipse.aether.connector.basic.*;
import org.eclipse.aether.graph.*;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.*;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.*;
import org.eclipse.aether.spi.connector.transport.*;
import org.eclipse.aether.transport.file.*;
import org.eclipse.aether.transport.http.*;
import org.eclipse.aether.util.artifact.*;
import org.eclipse.aether.util.filter.*;
import org.opensilex.utils.*;
import org.slf4j.*;

/**
 *
 * @author Vincent Migot
 */
public class DependencyManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(DependencyManager.class);

    private static String getArtifactKey(Artifact artifact) {
        String key = artifact.getGroupId()
                + ":" + artifact.getArtifactId()
                + ":" + artifact.getProperty("packaging", "jar")
                + ":" + artifact.getVersion();
        return key;
    }

    private static RepositorySystem getRepositorySystem() {
        DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator
                .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
        serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        serviceLocator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                // TODO use proper logs
                System.err.printf("error creating service: %s\n", exception.getMessage());
                exception.printStackTrace();
            }
        });

        return serviceLocator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
                .newSession();

        LocalRepository localRepository = new LocalRepository(".maven-dependencies");
        repositorySystemSession.setLocalRepositoryManager(
                system.newLocalRepositoryManager(repositorySystemSession, localRepository));

        repositorySystemSession.setRepositoryListener(new DependencyLogger());

        return repositorySystemSession;
    }

    private static RemoteRepository getCentralMavenRepository() {
        return new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    }

    private static List<RemoteRepository> getPomRemoteRepositories(Model model) {
        List<RemoteRepository> remoteRepoList = new ArrayList<>();
        remoteRepoList.add(getCentralMavenRepository());

        model.getRepositories().forEach((repo) -> {
            remoteRepoList.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
        });

        return remoteRepoList;
    }

    private RepositorySystem system;
    private RepositorySystemSession session;

    private final List<String> loadedDependencies = new ArrayList<>();
    private final List<String> buildinDependencies = new ArrayList<>();

    public DependencyManager(File mainPom) throws ModelBuildingException, DependencyResolutionException, MalformedURLException {
        initRegistries();
        loadDependencies(mainPom, false);
        buildinDependencies.addAll(loadedDependencies);
    }

    private void initRegistries() {
        system = getRepositorySystem();
        session = getRepositorySystemSession(system);
    }

    private Model registerPom(File pom) throws ModelBuildingException {
        Model model = buildPomModel(pom);

        loadedDependencies.add(model.getGroupId()
                + ":" + model.getArtifactId()
                + ":" + model.getPackaging()
                + ":" + model.getVersion());

        return model;
    }

    private List<URL> loadDependencies(File pom, boolean downloadWithMaven) throws DependencyResolutionException, ModelBuildingException, MalformedURLException {
        List<URL> resolvedDependencies = new ArrayList<>();
        Model model = registerPom(pom);

        LOGGER.debug(String.format("Maven model resolved: %s, parsing its dependencies...", model));

        for (org.apache.maven.model.Dependency d : model.getDependencies()) {
            if (!d.getType().equals("war")) {

                Artifact artifact = new DefaultArtifact(
                        d.getGroupId(),
                        d.getArtifactId(),
                        d.getClassifier(),
                        d.getType(),
                        d.getVersion()
                );

                String key = getArtifactKey(artifact);

                LOGGER.debug(String.format("Loading dependency: %s", key));

                if (!loadedDependencies.contains(key)) {
                    loadedDependencies.add(key);

                    if (downloadWithMaven) {
                        CollectRequest collectRequest = new CollectRequest(
                                new Dependency(artifact, JavaScopes.COMPILE),
                                getPomRemoteRepositories(model)
                        );

                        DependencyFilter filterScope = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
                        DependencyFilter filterPattern = new PatternExclusionsDependencyFilter(buildinDependencies);
                        DependencyRequest request = new DependencyRequest(collectRequest, DependencyFilterUtils.orFilter(filterScope, filterPattern));
                        DependencyResult results = system.resolveDependencies(session, request);

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

    public List<URL> loadModulesDependencies(List<URL> jarModulesURLs) throws IOException, DependencyResolutionException, ModelBuildingException {
        List<URL> dependenciesUrl = new ArrayList<>();
        for (URL jarURL : jarModulesURLs) {
            File pom = ClassInfo.getPomFile(jarURLToFile(jarURL));
            registerPom(pom);
        }

        for (URL jarURL : jarModulesURLs) {
            File jarFile = jarURLToFile(jarURL);
            dependenciesUrl.addAll(loadDependencies(ClassInfo.getPomFile(jarFile), true));
        }

        return dependenciesUrl;
    }

    private File jarURLToFile(URL jarURL) {
        File jarFile;
        try {
            jarFile = new File(jarURL.toURI());
        } catch (URISyntaxException e) {
            jarFile = new File(jarURL.getPath());
        }

        return jarFile;
    }

    private Model buildPomModel(File pom) throws ModelBuildingException {
        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setPomFile(pom);

        ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult modelBuildingResult = modelBuilder.build(modelBuildingRequest);

        Model model = modelBuildingResult.getEffectiveModel();

        return model;
    }
}
