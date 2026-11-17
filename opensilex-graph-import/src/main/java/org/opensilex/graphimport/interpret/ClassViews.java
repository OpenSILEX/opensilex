//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.ClassViewReader;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * The views of the classes one interpretation needs, each read once.
 *
 * @author Arnaud Charleroy
 */
public class ClassViews {

    /** How the views are read; a test gives its own. */
    @FunctionalInterface
    public interface Reader {
        ClassView read(URI classUri) throws Exception;
    }

    private final Reader reader;
    private final Map<URI, ClassView> views = new HashMap<>();

    public ClassViews(Reader reader) {
        this.reader = reader;
    }

    public static ClassViews of(ClassViewReader reader) {
        return new ClassViews(reader::read);
    }

    public ClassView get(URI classUri) throws Exception {
        ClassView view = views.get(classUri);
        if (view == null) {
            view = reader.read(classUri);
            views.put(classUri, view);
        }
        return view;
    }
}
