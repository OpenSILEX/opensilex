//**********************************************************************************************
//                                       SPARQLStringBuilder.java 
//
// Author(s): Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: A class which provide method to manipulate SPARQL 
//***********************************************************************************************
package phis2ws.service.utils.sparql;

import java.util.Iterator;
import java.util.List;

/**
 * Classe abstraite qui permet la création de constructeur qui facilite
 * l'écriture de requête SPARQL à partir de chaîne de caractères
 *
 * @author Arnaud Charleroy
 */
public abstract class SPARQLStringBuilder {

    protected String prefix;
    protected String select;
    protected String from = null;
    protected String body; // corps de la requête
    protected String parameters; // textes additionnels
    protected int blankNodeCounter = 0;

    protected String filter = "";
    Boolean sesameUriFormSubject = null;
    Boolean sesameUriFormPredicat = null;
    Boolean sesameUriFormObject = null;

    public SPARQLStringBuilder() {
        prefix ="";
        select = "";
        body = "";
        parameters = "";

    }

    public void appendToBody(String queryPart) {
        this.body += queryPart;
    }

    /**
     * Ajouts des préfix en amont
     *
     * @param localName
     * @param prefixUri
     */
    public void appendPrefix(String localName, String prefixUri) {
        if (this.prefix.length() > 0) {
            this.prefix  += "\n" ;
        }
        this.prefix  += "PREFIX " + localName + ":<" + prefixUri + "#>";
    }

    /**
     * Paramètres supplémentaires (Exemple LIMIT)
     *
     * @param parameters
     */
    public void appendParameters(String parameters) {
        if (this.parameters != null && this.parameters.length() > 0) {
            this.parameters += "\n";
        }
        this.parameters += " " + parameters + " ";
    }

    public void beginBodyOptional() {
        this.body  += "\n"+"OPTIONAL {"+"\n";
    }

    public void endBodyOptional() {
        this.body += "\n"+"}";
    }

    public void forceSubjectUriForm() {
        sesameUriFormSubject = true;
    }

    public void normalSubjectUriForm() {
        sesameUriFormSubject = null;
    }

    public void stringifySubjectForm() {
        sesameUriFormSubject = false;
    }

    public void forcePredicatUriForm() {
        sesameUriFormPredicat = true;
    }

    public void normalPredicatUriForm() {
        sesameUriFormPredicat = null;
    }

    public void stringifyPredicatForm() {
        sesameUriFormPredicat = false;
    }

    public void forceObjectUriForm() {
        sesameUriFormObject = true;
    }

    public void normalObjectUriForm() {
        sesameUriFormObject = null;
    }

    public void stringifyObjectForm() {
        sesameUriFormObject = false;
    }

    public void appendFilter(String filter) {
        this.filter += filter;
    }

    /**
     * Ajout du corps de la requête possibilité de mettre des semi colon
     *
     * @param subject
     * @param predicate
     * @param object
     * @param semicolon
     */
    public void appendTriplet(String subject, String predicate, String object, Boolean semicolon) {
        if (body != null && body.length() > 0) {
            body += "\n";
        }

        if (subject == null) {
            body += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (sesameUriFormSubject == null) {
            if (subject.contains(":") && !subject.contains("http://")) {
                body += " " + subject + " ";
            }
             else if (subject.contains("\"")) {
                body += " " + subject + " ";
            } else if ((subject.contains("<") && subject.contains(">")) || subject.contains("?")) {
                body += " " + subject + " ";
            } else {
                body += " <" + subject + "> ";
            }
        } else if (sesameUriFormSubject) {
            body += " <" + subject + "> ";
        } else {
            body += " \"" + subject + "\" ";
        }

        if (predicate == null) {
            body += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (sesameUriFormPredicat == null) {
            if (predicate.contains(":") && !predicate.contains("http://")) {
                body += " " + predicate + " ";
            } else if (predicate.contains("\"")) {
                body += " " + predicate + " ";
            } else if ((predicate.contains(">") && predicate.contains("<")) || predicate.contains("?")) {
                body += " " + predicate + " ";
            } else {
                body += " <" + predicate + "> ";
            }
        } else if (sesameUriFormPredicat) {
            body += " <" + predicate + "> ";
        } else {
            body += " \"" + predicate + "\" ";
        }

        if (object == null) {
            body += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (sesameUriFormObject == null) {
            if (object.contains(":") && !object.contains("http://")) {
                body += " " + object + " ";
            } else if (object.contains("'")){
                body += " " + object + " ";
            } 
            else if (object.contains("\"")) {
                body += " " + object + " ";
            } else if ((object.contains(">") && object.contains("<")) || object.contains("?")) {
                body += " " + object + " ";
            } else {
                body += " <" + object + ">";
            }
        } else if (sesameUriFormObject) {
            body += " <" + object + "> ";
        } else {
            body += " \"" + object + "\" ";
        }

        if (semicolon != null && semicolon) {
            body += " ;";
        } else {
            body += " . ";
        }
    }

    public void appendSimpleUnion(String subject, String predicate, List<String> unions) {

        Iterator<String> iteratorStringUnion = unions.iterator();
        while (iteratorStringUnion.hasNext()) {
            String union = iteratorStringUnion.next();

            if (body != null && body.length() > 0) {
                body += "\n";
            }
            if (unions.size() > 1) {
                body += " { ";
            }

            if (subject == null) {
                body += " ?" + blankNodeCounter + " ";
                blankNodeCounter++;
            } else if (sesameUriFormSubject == null) {
                if (subject.contains(":") && !subject.contains("http://")) {
                    body += " " + subject + " ";
                } else if (subject.contains("\"")) {
                    body += " " + subject + " ";
                } else if ((subject.contains("<") && subject.contains(">")) || subject.contains("?")) {
                    body += " " + subject + " ";
                } else {
                    body += " <" + subject + "> ";
                }
            } else if (sesameUriFormSubject) {
                body += " <" + subject + "> ";
            } else {
                body += " \"" + subject + "\" ";
            }

            if (predicate == null) {
                body += " ?" + blankNodeCounter + " ";
                blankNodeCounter++;
            } else if (sesameUriFormPredicat == null) {
                if (predicate.contains(":") && !predicate.contains("http://")) {
                    body += " " + predicate + " ";
                } else if (predicate.contains("\"")) {
                    body += " " + predicate + " ";
                } else if ((predicate.contains(">") && predicate.contains("<")) || predicate.contains("?")) {
                    body += " " + predicate + " ";
                } else {
                    body += " <" + predicate + "> ";
                }
            } else if (sesameUriFormPredicat) {
                body += " <" + predicate + "> ";
            } else {
                body += " \"" + predicate + "\" ";
            }

            if (union == null) {
                body += " ?" + blankNodeCounter + " ";
                blankNodeCounter++;
            } else if (sesameUriFormObject == null) {
                if (union.contains(":") && !union.contains("http://")) {
                    body += " " + union + " ";
                } else if (union.contains("\"")) {
                    body += " " + union + " ";
                } else if ((union.contains(">") && union.contains("<")) || union.contains("?")) {
                    body += " " + union + " ";
                } else {
                    body += " <" + union + ">";
                }
            } else if (sesameUriFormObject) {
                body += " <" + union + "> ";
            } else {
                body += " \"" + union + "\" ";
            }
            if (unions.size() > 1) {
                body += " }";
                if (iteratorStringUnion.hasNext()) {
                    body += " UNION ";
                }
            }else if(unions.size() == 1){
                body += " . ";
            }
        }
    }

    /**
     *
     */
    protected void clearBuilder() {
        blankNodeCounter = 0;
        body = "";
        from = "";
        parameters = "";
        prefix = "";
        select = "";
        sesameUriFormObject = null;
        sesameUriFormPredicat = null;
        sesameUriFormSubject = null;
    }
}
