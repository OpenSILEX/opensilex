//******************************************************************************
//                                       Namespace.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 13 juil. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  13 juil. 2018
// Subject: Represent a namespace model
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * Represent a namespace with the corresponding prefix
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class Namespace implements Comparable<Namespace>{

    private String prefix; // The prefix of the namespace .e.g oa
    private String namespace; // The namespace .e.g https://www.w3.org/ns/oa

    public Namespace(String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
    }
    
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Used to compare two namespace.
     * Compares the two prefix strings lexicographically.
     * Use to sort Arraylist<Namespace> in VocabularyDao
     * e.g. Collections.sort(arraylist)
     * @param namespaceCompared
     * @return 
     */
    @Override
    public int compareTo(Namespace namespaceCompared) {
        return this.getPrefix().compareTo(namespaceCompared.getPrefix());
    }

}
