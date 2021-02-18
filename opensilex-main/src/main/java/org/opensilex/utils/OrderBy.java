//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import org.apache.commons.text.CaseUtils;
import org.apache.jena.arq.querybuilder.Order;

/**
 * Order by data type to manage an order by parameter with its direction (ASC/DESC).
 *
 * @author Vincent Migot
 */
public class OrderBy {

    /**
     * Name of the ordered field.
     */
    private final String fieldName;

    /**
     * Order direction.
     */
    private final Order order;

    /**
     * Constructor from string.
     *
     * @param orderByStr string matching fieldname=direction pattern.
     * @throws Exception
     */
    public OrderBy(String orderByStr) throws Exception {
        String[] orderParts = orderByStr.split("=");
        if (orderParts.length <= 2 && orderParts[0].length() > 0) {
            // "camel_case" returns "camelCase"
            if (orderParts[0].contains("_")) {
                fieldName = CaseUtils.toCamelCase(orderParts[0], false, new char[]{'_'});
            } else {
                fieldName = orderParts[0];
            }
            
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

    /**
     * Getter for field name.
     *
     * @return field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Getter for field order.
     *
     * @return field order.
     */
    public Order getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return fieldName + ":" + order.toString(); //To change body of generated methods, choose Tools | Templates.
    }
}
