/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.skos.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;

/**
 *
 * @author charlero
 */
public class AbstractSkosModelTest extends AbstractSecurityIntegrationTest{
    
    public void setValidSkosReferences(SKOSReferencesDTO dto) throws URISyntaxException{
        List<URI> skosRefererences = new ArrayList<>();
        skosRefererences.add(new URI("http://www.w3.org/TR/2004/REC-owl-test-20040210/"));
        skosRefererences.add(new URI("http://www.w3.org/TR/owl-test/"));
        dto.setBroader(skosRefererences);
    }
    
    public void setWrongSkosReferences(SKOSReferencesDTO dto) throws URISyntaxException{
        List<URI> skosRefererences = new ArrayList<>();
        skosRefererences.add(new URI("asasa"));
        skosRefererences.add(new URI("asasasas"));
        dto.setBroader(skosRefererences);
    }
}
