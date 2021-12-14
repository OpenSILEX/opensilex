/*******************************************************************************
 *                         OntologyStoreCoreTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.phis.ontology;


import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.store.DefaultOntologyStore;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;

public class OntologyStoreCoreTest extends AbstractSecurityIntegrationTest {

    private OntologyStore store;

    @Before
    public void initStore() throws Exception {
        SPARQLService sparql = getSparqlService();
        store = new DefaultOntologyStore(sparql, Arrays.asList("en", "fr", ""));

        int n=10000;
        List<ClassModel> classes = new ArrayList<>(n);

        for(int i=0;i<n;i++){
            ClassModel classModel = new ClassModel();
            classModel.setUri(URI.create("test:OntologyStoreCoreTest"+i));

            SPARQLLabel label = new SPARQLLabel();

            Map<String,String> labels = new HashMap<>();
            labels.put("en","class_label_en"+i);
            labels.put("fr","class_label_fr"+i);
            labels.put("","class_label_no_lang"+i);

            label.setTranslations(labels);
            classModel.setLabel(label);

            SPARQLLabel comment = new SPARQLLabel();
            Map<String,String> comments = new HashMap<>();
            comments.put("en","class_comment_en"+i);
            comments.put("fr","class_comment_fr"+i);
            comments.put("","class_comment_no_lang"+i);

            comment.setTranslations(comments);
            classModel.setComment(comment);

            classes.add(classModel);
        }

        sparql.create(ClassModel.class,classes);
    }

    @Test
    @Ignore("Simple naive performance test")
    public void testOntologyStoreLoadingPerformance() throws SPARQLException {
        for(int i=0 ;i<10; i++){
            store.load();
            store.clear();
        }
    }

    @After
    public void clearStore(){
    }
}
