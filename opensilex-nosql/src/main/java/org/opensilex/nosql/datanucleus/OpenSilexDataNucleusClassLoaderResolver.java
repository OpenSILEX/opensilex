/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.datanucleus;

import org.datanucleus.ClassLoaderResolverImpl;
import org.opensilex.OpenSilex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class OpenSilexDataNucleusClassLoaderResolver extends ClassLoaderResolverImpl {

    public final static Logger LOGGER = LoggerFactory.getLogger(OpenSilexDataNucleusClassLoaderResolver.class);

    public OpenSilexDataNucleusClassLoaderResolver() {
        super(OpenSilex.getClassLoader());
        LOGGER.info("Iinit custom class loader resolver");
    }

}
