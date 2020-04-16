/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.scanner;

import javax.servlet.ServletContext;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;

/**
 * Jar scanner for tomcat to avoid unnecessary JAR inspection.
 *
 * @author Vincent Migot
 */
public class IgnoreJarScanner implements JarScanner {

    /**
     * Scan filter.
     */
    private JarScanFilter jarScanFilter = (JarScanType jst, String string) -> false;

    @Override
    public void scan(JarScanType jst, ServletContext sc, JarScannerCallback jsc) {
        // do nothing
    }

    @Override
    public JarScanFilter getJarScanFilter() {
        return jarScanFilter;
    }

    @Override
    public void setJarScanFilter(JarScanFilter jsf) {
        // do nothing
    }

}
