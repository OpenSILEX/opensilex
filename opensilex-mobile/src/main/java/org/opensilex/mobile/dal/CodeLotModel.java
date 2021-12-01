//******************************************************************************
//                          CodeLotModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.dal;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * This class builds objects that represent an available unused CodeLot with all
 * it's available children. When a code lot gets used and assosiated to a form
 * type, this object is deleted and the available children are passed to the
 * form.
 *
 * @author Maximilian Hart
 */
public class CodeLotModel extends MongoModel {

    private final String head;
    private final List<CodeLotModel> availableChildren;
    private final List<CodeLotModel> parents;

    public CodeLotModel(String head) {
        this.head = head;
        this.availableChildren = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    public String getHead() {
        return head;
    }

    public List<CodeLotModel> getParents() {
        return parents;
    }

//    public void setHead(String head) {
//        this.head = head;
//    }
    public List<CodeLotModel> getAvailableChildren() {
        return availableChildren;
    }

    public void addStringChild(String child) {
        this.availableChildren.add(new CodeLotModel(child));
    }

    public void addChild(CodeLotModel child) {
        this.availableChildren.add(child);
    }

    public void addParent(CodeLotModel parent) {
        this.parents.add(parent);
    }

    @Override
    public String[] getUriSegments(MongoModel instance) {
        return new String[]{
            "code_lot_head:" + head
        };
    }

    public boolean containsChild(String child) {
        boolean found = false;
        int i = 0;
        while (!found && i < availableChildren.size()) {
            if (availableChildren.get(i).getHead().equals(child)) {
                found = true;
            }
            i++;
        }
        return found;
    }
}
