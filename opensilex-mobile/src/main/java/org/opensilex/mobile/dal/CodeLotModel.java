//******************************************************************************
//                          CodeLotModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * This class builds objects that represent an available unused CodeLot with all
 * it's available children. When a code lot gets used and assosiated to a form
 * type, this object is deleted and the available children are passed to the
 * form.
 *
 * @author Maximilian Hart
 */
public class CodeLotModel {

    private final String head;
    private final URI type;
    private final List<CodeLotModel> children;
    private final List<CodeLotModel> parents;

    public CodeLotModel(String head, URI type) {
        this.head = head;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
        this.type = type;
    }

    public String getHead() {
        return head;
    }

    public List<CodeLotModel> getParents() {
        return parents;
    }

    public URI getType() {
        return type;
    }

    public List<CodeLotModel> getChildren() {
        return children;
    }

    public void addChild(CodeLotModel child) {
        this.children.add(child);
    }

    public void addParent(CodeLotModel parent) {
        this.parents.add(parent);
    }

    public boolean containsChild(String child) {
        boolean found = false;
        int i = 0;
        while (!found && i < children.size()) {
            if (children.get(i).getHead().equals(child)) {
                found = true;
            }
            i++;
        }
        return found;
    }
}
