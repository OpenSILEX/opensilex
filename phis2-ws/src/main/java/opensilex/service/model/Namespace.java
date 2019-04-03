//******************************************************************************
//                              Namespace.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 Jul. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Namespace with prefix model.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class Namespace implements Comparable<Namespace>{

    /**
     * Prefix of the namespace.
     * @example oa
     */
    private String prefix;
    
    /**
     * Namespace name.
     * @example https://www.w3.org/ns/oa
     */
    private String namespace;

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
     * Compares two namespaces.
     * Compares the two prefix strings lexicographically.
     * Used to sort Arraylist<Namespace> in VocabularyDao
     * e.g. Collections.sort(arraylist)
     * @param namespaceCompared
     * @return 
     */
    @Override
    public int compareTo(Namespace namespaceCompared) {
        return this.getPrefix().compareTo(namespaceCompared.getPrefix());
    }
}
