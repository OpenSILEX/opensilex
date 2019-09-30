/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.dependency;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Vincent Migot
 */
public class DependencyLogger
        extends AbstractRepositoryListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(DependencyManager.class);
    
    @Override
    public void artifactInstalled(RepositoryEvent event) {
        LOGGER.debug(String.format("Artifact %s installed to file %s", event.getArtifact(), event.getFile()));
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        LOGGER.debug(String.format("Installing artifact %s to file %s", event.getArtifact(), event.getFile()));
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        LOGGER.debug(String.format("Artifact %s resolved from repository %s", event.getArtifact(),
                event.getRepository()));
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        LOGGER.debug(String.format("Downloading artifact %s from repository %s", event.getArtifact(),
                event.getRepository()));
    }

    @Override
    public void artifactDownloaded(RepositoryEvent event) {
        LOGGER.debug(String.format("Downloaded artifact %s from repository %s", event.getArtifact(),
                event.getRepository()));
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        LOGGER.debug(String.format("Resolving artifact %s", event.getArtifact()));
    }

     @Override
    public void metadataResolving(RepositoryEvent event) {
        LOGGER.debug(String.format("Resolving artifact metadata %s", event.getArtifact()));
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
         LOGGER.debug(String.format("Artifact %s metadata resolved", event.getArtifact()));
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        LOGGER.error(String.format("Artifact %s metadata invalid", event.getArtifact()));
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        LOGGER.debug(String.format("Installing artifact %s metadata", event.getArtifact()));
    }

    @Override
    public void metadataInstalled(RepositoryEvent event) {
        LOGGER.debug(String.format("Artifact %s metadata installed", event.getArtifact()));
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        LOGGER.debug(String.format("Downloading artifact %s metadata", event.getArtifact()));
    }

    @Override
    public void metadataDownloaded(RepositoryEvent event) {
         LOGGER.debug(String.format("Artifact %s metadata downloaded", event.getArtifact()));
    }

    @Override
    public void metadataDeploying(RepositoryEvent event) {
        LOGGER.debug(String.format("Deploying artifact %s metadata", event.getArtifact()));
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
         LOGGER.debug(String.format("Artifact %s metadata deployed", event.getArtifact()));
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        LOGGER.error(String.format("Artifact %s descriptor missing", event.getArtifact()));
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        LOGGER.error(String.format("Artifact %s descriptor invalid", event.getArtifact()));
    }

    @Override
    public void artifactDeploying(RepositoryEvent event) {
        LOGGER.debug(String.format("Deploying artifact %s", event.getArtifact()));
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
         LOGGER.debug(String.format("Artifact %s deployed", event.getArtifact()));
    }
}
