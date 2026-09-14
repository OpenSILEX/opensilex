//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.core.variable.bll;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * What a copy from a shared resource instance created here.
 * <p>
 * The components are listed apart from the variables because they are the interesting part: a
 * caller usually asks for two variables and finds that four entities and three units came with
 * them, which is what makes copying cheaper than creating.
 *
 * @author Arnaud Charleroy
 */
public class VariableCopyResult {

    private List<URI> variableUris = new ArrayList<>();
    private List<URI> entityUris = new ArrayList<>();
    private List<URI> interestEntityUris = new ArrayList<>();
    private List<URI> characteristicUris = new ArrayList<>();
    private List<URI> methodUris = new ArrayList<>();
    private List<URI> unitUris = new ArrayList<>();

    public List<URI> getVariableUris() {
        return variableUris;
    }

    public void setVariableUris(List<URI> variableUris) {
        this.variableUris = variableUris;
    }

    public List<URI> getEntityUris() {
        return entityUris;
    }

    public void setEntityUris(List<URI> entityUris) {
        this.entityUris = entityUris;
    }

    public List<URI> getInterestEntityUris() {
        return interestEntityUris;
    }

    public void setInterestEntityUris(List<URI> interestEntityUris) {
        this.interestEntityUris = interestEntityUris;
    }

    public List<URI> getCharacteristicUris() {
        return characteristicUris;
    }

    public void setCharacteristicUris(List<URI> characteristicUris) {
        this.characteristicUris = characteristicUris;
    }

    public List<URI> getMethodUris() {
        return methodUris;
    }

    public void setMethodUris(List<URI> methodUris) {
        this.methodUris = methodUris;
    }

    public List<URI> getUnitUris() {
        return unitUris;
    }

    public void setUnitUris(List<URI> unitUris) {
        this.unitUris = unitUris;
    }

    /**
     * @return how many resources the copy created in all, variables included
     */
    public int total() {
        return variableUris.size() + entityUris.size() + interestEntityUris.size()
                + characteristicUris.size() + methodUris.size() + unitUris.size();
    }
}
