//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import org.apache.jena.arq.querybuilder.Order;


/**
 *
 * @author vidalmor
 */
public class OrderBy {

    private final String fieldName;

    private final Order order;

    public OrderBy(String orderByStr) throws Exception {
        String[] orderParts = orderByStr.split("=");
        if (orderParts.length <= 2 && orderParts[0].length() > 0) {
            fieldName = orderParts[0];
            String fieldOrder = orderParts[1];
            if (fieldOrder.equalsIgnoreCase("desc") || fieldOrder.equals("1") || fieldOrder.equalsIgnoreCase("true")) {
                order = Order.DESCENDING;
            } else {
                order = Order.ASCENDING;
            }
        } else {
            throw new Exception("Invalid sort by element: " + orderByStr);
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    public Order getOrder() {
        return order;
    }
    
    
}
