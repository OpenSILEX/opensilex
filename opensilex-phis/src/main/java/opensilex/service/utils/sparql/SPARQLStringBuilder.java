//******************************************************************************
//                             SPARQLStringBuilder.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils.sparql;

import java.util.Iterator;
import java.util.List;
import opensilex.service.resource.validation.validator.URLValidator;

/**
 * Abstract class to manipulate SPARQL query.
 * @update [Arnaud Charleroy] 10 Sept. 2018: Add "And" and "Or" filter choices
 * @update [Vincent Migot] 03 Oct. 2018: Create isLink method to check URIs 
 * instead of only testing "http://" prefix
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public abstract class SPARQLStringBuilder {

    protected String prefix;
    protected String select;
    protected String from = null;
    protected String body; // body request
    protected String parameters; // additionnal text
    protected int blankNodeCounter = 0;

    protected String filter = "";
    Boolean tripleStoreUriFormSubject = null;
    Boolean tripleStoreUriFormPredicat = null;
    Boolean tripleStoreUriFormObject = null;

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
     * Append a prefix.
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
     * Appends an additional parameter.
     * @example LIMIT
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
        tripleStoreUriFormSubject = true;
    }

    public void normalSubjectUriForm() {
        tripleStoreUriFormSubject = null;
    }

    public void stringifySubjectForm() {
        tripleStoreUriFormSubject = false;
    }

    public void forcePredicatUriForm() {
        tripleStoreUriFormPredicat = true;
    }

    public void normalPredicatUriForm() {
        tripleStoreUriFormPredicat = null;
    }

    public void stringifyPredicatForm() {
        tripleStoreUriFormPredicat = false;
    }

    public void forceObjectUriForm() {
        tripleStoreUriFormObject = true;
    }

    public void normalObjectUriForm() {
        tripleStoreUriFormObject = null;
    }

    public void stringifyObjectForm() {
        tripleStoreUriFormObject = false;
    }

    /**
     * @example
     * FILTER ( (regex(STR(?creator), 'admin', 'i') ) 
     * @param filter the filter which will be applied e.g. regex(STR(?creator), 'admin', 'i')
     */
    public void appendFilter(String filter) {
        this.filter +=  "(" + filter + ")";
    }

    /**
     * @example
     * FILTER ( (regex(STR(?creator), 'admin', 'i')) && (regex(STR(?title), 'liste', 'i')) ) 
     * @param filter the filter which will be applied e.g. regex(STR(?creator), 'admin', 'i')
     */
    public void appendAndFilter(String filter) {
        if(this.filter != "") {
             this.filter += " && (" + filter  + ")";
        } else {
            this.appendFilter(filter);
        }
    }

    /**
     * @example
     * FILTER ( (regex(STR(?creator), 'admin', 'i')) || (regex(STR(?title), 'liste', 'i')) ) 
     * @param filter the filter which will be applied e.g. regex(STR(?creator), 'admin', 'i')
     */
    public void appendOrFilter(String filter) {
        if(this.filter != "") {
             this.filter += " || (" + filter  + ")";
        } else {
            this.appendFilter(filter);
        }
    }
    
    /**
     * Appends a triplet. Semicolons enabled.
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
        } else if (tripleStoreUriFormSubject == null) {
            if (subject.contains(":") && !isLink(subject)) {
                body += " " + subject + " ";
            }
             else if (subject.contains("\"")) {
                body += " " + subject + " ";
            } else if ((subject.contains("<") && subject.contains(">")) || subject.contains("?")) {
                body += " " + subject + " ";
            } else {
                body += " <" + subject + "> ";
            }
        } else if (tripleStoreUriFormSubject) {
            body += " <" + subject + "> ";
        } else {
            body += " \"" + subject + "\" ";
        }

        if (predicate == null) {
            body += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (tripleStoreUriFormPredicat == null) {
            if (predicate.contains(":") && !isLink(predicate)) {
                body += " " + predicate + " ";
            } else if (predicate.contains("\"")) {
                body += " " + predicate + " ";
            } else if ((predicate.contains(">") && predicate.contains("<")) || predicate.contains("?")) {
                body += " " + predicate + " ";
            } else {
                body += " <" + predicate + "> ";
            }
        } else if (tripleStoreUriFormPredicat) {
            body += " <" + predicate + "> ";
        } else {
            body += " \"" + predicate + "\" ";
        }

        if (object == null) {
            body += " ?" + blankNodeCounter + " ";
            blankNodeCounter++;
        } else if (tripleStoreUriFormObject == null) {
            if (object.contains(":") && !isLink(object)) {
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
        } else if (tripleStoreUriFormObject) {
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
            } else if (tripleStoreUriFormSubject == null) {
                if (subject.contains(":") && !isLink(subject)) {
                    body += " " + subject + " ";
                } else if (subject.contains("\"")) {
                    body += " " + subject + " ";
                } else if ((subject.contains("<") && subject.contains(">")) || subject.contains("?")) {
                    body += " " + subject + " ";
                } else {
                    body += " <" + subject + "> ";
                }
            } else if (tripleStoreUriFormSubject) {
                body += " <" + subject + "> ";
            } else {
                body += " \"" + subject + "\" ";
            }

            if (predicate == null) {
                body += " ?" + blankNodeCounter + " ";
                blankNodeCounter++;
            } else if (tripleStoreUriFormPredicat == null) {
                if (predicate.contains(":") && !isLink(predicate)) {
                    body += " " + predicate + " ";
                } else if (predicate.contains("\"")) {
                    body += " " + predicate + " ";
                } else if ((predicate.contains(">") && predicate.contains("<")) || predicate.contains("?")) {
                    body += " " + predicate + " ";
                } else {
                    body += " <" + predicate + "> ";
                }
            } else if (tripleStoreUriFormPredicat) {
                body += " <" + predicate + "> ";
            } else {
                body += " \"" + predicate + "\" ";
            }

            if (union == null) {
                body += " ?" + blankNodeCounter + " ";
                blankNodeCounter++;
            } else if (tripleStoreUriFormObject == null) {
                if (union.contains(":") && !isLink(union)) {
                    body += " " + union + " ";
                } else if (union.contains("\"")) {
                    body += " " + union + " ";
                } else if ((union.contains(">") && union.contains("<")) || union.contains("?")) {
                    body += " " + union + " ";
                } else {
                    body += " <" + union + ">";
                }
            } else if (tripleStoreUriFormObject) {
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
    
    protected void clearBuilder() {
        blankNodeCounter = 0;
        body = "";
        from = "";
        parameters = "";
        prefix = "";
        select = "";
        tripleStoreUriFormObject = null;
        tripleStoreUriFormPredicat = null;
        tripleStoreUriFormSubject = null;
    }
    
    private boolean isLink(String link) {
        return URLValidator.validateURL(link);
    }
}
