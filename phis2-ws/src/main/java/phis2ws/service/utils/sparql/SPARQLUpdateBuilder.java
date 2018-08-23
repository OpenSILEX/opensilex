//**********************************************************************************************
//                                       SPARQLUpdateBuilder.java 
//
// Author(s): Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: A class which permit to build SPARQL update query
//***********************************************************************************************
package phis2ws.service.utils.sparql;

/**
 * Classe concrète qui permet de créer des requêtes d'insertion en SPARQL
 *
 * @author Arnaud Charleroy
 */
public class SPARQLUpdateBuilder extends SPARQLStringBuilder {

    private String whereBody;
    private String add;
    private String graph;

    public SPARQLUpdateBuilder() {
        super();
        graph = "";
        whereBody = "";
        add = "";
    }

    /**
     * Specifiy a particular graph
     * @param graph 
     */
    public void appendGraphURI(String graph) {
        if (graph != null) {
            if (graph.contains("<") && graph.contains(">")) {
                this.graph += "GRAPH " + graph + " { ";
            }
            this.graph += "GRAPH <" + graph + "> { ";
        }
    }
    
    public void appendUpdateWhereTriplet(String subject, String predicate, String object, Boolean semicolon) {
        if (whereBody.length() > 0) {
            whereBody += "\n";
        }
        if (subject == null) {
            whereBody += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (subject.contains(":") && !subject.contains("http://")) {
            whereBody += " " + subject + " ";
        }else if (subject.contains("\"")) {
            whereBody += " " + subject + " ";
        }else if ((subject.contains("<") && subject.contains(">")) || subject.contains("?")) {
            whereBody += " " + subject + " ";
        }else{
            whereBody += " <" + subject + "> ";
        }
        
        if (predicate == null) {
            whereBody += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (predicate.contains(":") && !predicate.contains("http://")) {
            whereBody += " " + predicate + " ";
        }else if (predicate.contains("\"")) {
            whereBody += " " + predicate + " ";
        }else if ((predicate.contains(">") && predicate.contains("<")) || predicate.contains("?")) {
            whereBody += " " + predicate + " ";
        }else{
            whereBody += " <" + predicate + "> ";
        }
        
        if (object == null) {
            whereBody += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (object.contains(":") && !object.contains("http://")) {
            whereBody += " " + object + " ";
        }else if (object.contains("\"")) {
            whereBody += " " + object + " ";
        }else if ((object.contains(">") && object.contains("<")) || object.contains("?")) {
            whereBody += " " + object + " ";
        }else{
            whereBody += " <" + object + ">";
        }
        if (semicolon != null && semicolon) {
            whereBody += " ;";
        }else{
            whereBody += " .";
        }
    }
    
    @Override
    public String toString() {
        String addResource = "";
        if (prefix != null) {
            addResource += prefix + "\n";
        }
        addResource += "INSERT DATA {" + "\n";
        if (graph.length() > 0) {
            addResource += graph + "\n";
        }
        addResource += body + "\n";
        if (whereBody.length() > 0) {
            addResource += " WHERE {";
                addResource += whereBody;
                addResource += "}";
            }
        if (graph.length() > 0) {
            addResource += "}";
        }
        addResource += "}";
        return addResource;
    }

    @Override
    public void clearBuilder() {
        super.clearBuilder();
        add = "";
        graph = "";
        whereBody = "";

    }
}
