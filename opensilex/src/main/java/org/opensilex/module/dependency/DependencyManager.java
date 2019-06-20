/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.dependency;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
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
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class DependencyManager {

    
    private final static Logger LOGGER = LoggerFactory.getLogger(DependencyManager.class);
    
    public DependencyManager(File mainPom) throws ModelBuildingException, DependencyResolutionException, MalformedURLException {
        initRegistries();
        registerDependencies(mainPom, false);
        buildinDependencies.addAll(loadedDependencies);
    }

    private RepositorySystem system;
    private RepositorySystemSession session;

    private void initRegistries() {
        system = getRepositorySystem();
        session = getRepositorySystemSession(system);
    }

    private List<String> loadedDependencies = new ArrayList<>();
    private List<String> buildinDependencies = new ArrayList<>();

    private List<URL> registerDependencies(File pom, boolean loadDependencies) throws DependencyResolutionException, ModelBuildingException, MalformedURLException {
        List<URL> resolvedDependencies = new ArrayList<>();
        Model model = buildPomModel(pom);

        LOGGER.debug(String.format("Maven model resolved: %s, parsing its dependencies...", model));

        loadedDependencies.add(model.getGroupId()
                + ":" + model.getArtifactId()
                + ":" + model.getPackaging()
                + ":" + model.getVersion());

        for (org.apache.maven.model.Dependency d : model.getDependencies()) {
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

                if (loadDependencies) {
                    CollectRequest collectRequest = new CollectRequest(
                            new Dependency(artifact, JavaScopes.COMPILE),
                            getPomRemoteRepositories(model)
                    );

                    DependencyFilter filterScope = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
                    DependencyFilter filterPattern = new PatternExclusionsDependencyFilter(buildinDependencies);
                    DependencyRequest request = new DependencyRequest(collectRequest, filterScope);
                    DependencyResult results = system.resolveDependencies(session, request);

                    for (ArtifactResult result : results.getArtifactResults()) {
                        if (result.getArtifact().getGroupId().equals("com.jcraft")) {
                            System.out.println("DA FUCK ??");
                        }
                        if (result.isResolved()) {
                            Artifact resultArtifact = result.getArtifact();
                            loadedDependencies.add(getArtifactKey(resultArtifact));
                            resolvedDependencies.add(resultArtifact.getFile().toURI().toURL());
                        }
                    };
                }
            }
        }

        return resolvedDependencies;
    }

    private static String getArtifactKey(Artifact artifact) {
        String key = artifact.getGroupId()
                + ":" + artifact.getArtifactId()
                + ":" + artifact.getProperty("packaging", "jar")
                + ":" + artifact.getVersion();
        return key;
    }

    public List<URL> resolveDependencies(List<URL> jarURLs) throws IOException, DependencyResolutionException, ModelBuildingException {
        List<URL> dependenciesUrl = new ArrayList<>();
        for (URL jarURL : jarURLs) {
            dependenciesUrl.addAll(resolveDependencies(jarURL, null, null));
        }
        
        return dependenciesUrl;
    }
        
    public List<URL> resolveDependencies(URL jarURL) throws IOException, DependencyResolutionException, ModelBuildingException {
        return resolveDependencies(jarURL, null, null);
    }

    public List<URL> resolveDependencies(URL jarURL, String groupId, String artifactId) throws IOException, DependencyResolutionException, ModelBuildingException {
        File jarFile;
        try {
            jarFile = new File(jarURL.toURI());
        } catch (URISyntaxException e) {
            jarFile = new File(jarURL.getPath());
        }

        return registerDependencies(ClassInfo.getPomFile(jarFile, groupId, artifactId), true);
    }

    private Model buildPomModel(File pom) throws ModelBuildingException {
        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setPomFile(pom);

        ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult modelBuildingResult = modelBuilder.build(modelBuildingRequest);

        Model model = modelBuildingResult.getEffectiveModel();

        return model;
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
                System.err.printf("error creating service: %s\n", exception.getMessage());
                exception.printStackTrace();
            }
        });

        return serviceLocator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
                .newSession();

        LocalRepository localRepository = new LocalRepository("target/local-repository");
        repositorySystemSession.setLocalRepositoryManager(
                system.newLocalRepositoryManager(repositorySystemSession, localRepository));

        repositorySystemSession.setRepositoryListener(new DependencyLogger());

        return repositorySystemSession;
    }

    private static List<RemoteRepository> getRepositories(RepositorySystem system,
            RepositorySystemSession session) {
        return Arrays.asList(getCentralMavenRepository());
    }

    private static RemoteRepository getCentralMavenRepository() {
        return new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    }

    private static List<RemoteRepository> getPomRemoteRepositories(Model model) {
        List<RemoteRepository> remoteRepoList = new ArrayList<>();
        remoteRepoList.add(getCentralMavenRepository());

        for (Repository repo : model.getRepositories()) {
            remoteRepoList.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
        }

        return remoteRepoList;
    }
}
