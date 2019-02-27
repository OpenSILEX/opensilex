//**********************************************************************************************
//                                       SQLQueryBuilder.java 
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  February, 2017
// Subject: A class which permit to build a SQL query
//***********************************************************************************************
package phis2ws.service.utils.sql;

import java.util.Iterator;
import java.util.List;

/**
 * Classe concrète qui permet de créer des requêtes de recherche en SQL
 *
 * @author Arnaud Charleroy, Morgane Vidal
 */
public class SQLQueryBuilder {

    public static final String CONTAINS_OPERATOR = "CONTAINS_OPERATOR";
    
    private boolean count = false;
    private boolean distinct = false;
    private String attributes = null;
    public String from;
    public String where;
    public String join;
    public String orderBy;
    public String limit;
    public String offset;
//    public static JoinAttributes JoinAttributes = new JoinAttributes(;

    public SQLQueryBuilder() {
        where = "";
        from = "";
        join = "";
        orderBy = "";
        limit = "";
        offset = "";
    }

//    public JoinAttributes getJoinAttributes(){
//        return JoinAttributes;
//    }
    public void appendOrderBy(String valuesList, String order) {
        if (valuesList != null) {
            orderBy += "\n" + valuesList + "\n";
            if (order != null) {
                orderBy += " " + order + " ";
            }
        }
    }

    public void appendCount() {
        count = true;
    }

    public void appendDistinct() {
        distinct = true;
    }

    public void removeDistinct() {
        distinct = true;
    }

    public void removeCount() {
        count = false;
    }

    public void addAND() {
        where += " AND ";
    }

    public void addOR() {
        where += " OR ";
    }

    /**
     * Ajout du sélect
     *
     * @param values
     */
    public void appendSelect(String values) {
        if (values != null) {
            attributes = values;
        }
    }

    public void addISNULL(String attribute) {
        if (where.length() > 0) {
                this.where += " AND ";
            }
        where += attribute + " IS NULL ";
    }
    
    public void appendJoin(String joinName, String table, String tableAlias, String constraint) {
        if (table != null && constraint != null && joinName != null) {
            join += "\n";
            if (tableAlias != null) {
                join += joinName + " \"" + table + "\" AS " + tableAlias;
            } else {
                join += joinName + " \"" + table + "\"";
            }
            if (!joinName.equals(JoinAttributes.NATURALJOIN)) {
                join += " ON " + constraint;
            }
        }
    }

    /**
     * Ajout du from
     *
     * @param table
     * @param alias
     */
    public void appendFrom(String table, String alias) {
        if (table != null) {
            if (from.length() > 0) {
                from += ", \"" + table + "\" ";
            } else {
                from += " FROM \"" + table + "\" ";
            }
            if (alias != null) {
                from += "AS " + alias + " ";
            }
        }
    }

public void appendORWhereConditions(String attribute, String value, String operator, String type, String tableAlias) {
        if (attribute != null && value != null) {
            if (CONTAINS_OPERATOR.equals(operator)) {
                operator = "ILIKE";
                value = "%" + value + "%";
            }
            if (where.length() > 0) {
                this.where += " OR ";
            }
            if (tableAlias != null) {
                this.where += tableAlias + "." + "\"" + attribute + "\"";
            } else {
                this.where += "\"" + attribute + "\"";
            }

            if (operator != null) {
                this.where += " " + operator + " ";
            } else {
                this.where += " = ";
            }
            this.where += "'" + value + "'";
            if (type != null) {
                this.where += type;
            }
        }
    }

    public void appendINConditions(String attribute, List<String> valuesGroup, String tableAlias) {
        if (valuesGroup != null) {
            if (where.length() > 0) {
                this.where += " AND ";
            }
            if (valuesGroup.size() > 1) {
                if (tableAlias != null) {
                    this.where += tableAlias + "." + "\"" + attribute + "\" IN (";
                } else {
                    this.where += "\"" + attribute + "\" IN (";
                }
                Iterator<String> it = valuesGroup.iterator();
                while (it.hasNext()) {
                    this.where += "'" + it.next() + "'";
                    if (it.hasNext()) {
                        this.where += ",";
                    }
                }
                this.where += " " + ") ";
            } else if (valuesGroup.size() == 1) {
                if (tableAlias != null) {
                    this.appendPrivateWhereConditions(attribute, valuesGroup.get(0), "=", null, tableAlias);
                } else {
                    this.appendPrivateWhereConditions(attribute, valuesGroup.get(0), "=", null, null);
                }
            }
        }
    }

    public void appendWhereConditions(String attribute, String value, String operator, String type, String tableAlias) {
        if (attribute != null && value != null) {
            if (CONTAINS_OPERATOR.equals(operator)) {
                operator = "ILIKE";
                value = "%" + value + "%";
            }
            if (tableAlias != null) {
                this.where += tableAlias + "." + "\"" + attribute + "\"";
            } else {
                this.where += "\"" + attribute + "\"";
            }

            if (operator != null) {
                this.where += " " + operator + " ";
            } else {
                this.where += " = ";
            }
            this.where += "'" + value + "'";
            if (type != null) {
                this.where += type;
            }
        }
    }

    public void appendANDWhereConditions(String attribute, String value, String operator, String type, String tableAlias) {
        if (attribute != null && value != null) {
            if (CONTAINS_OPERATOR.equals(operator)) {
                operator = "ILIKE";
                value = "%" + value + "%";
            }
            
            if (where.length() > 0) {
                this.where += " AND ";
            }
            if (tableAlias != null) {
                this.where += tableAlias + "." + "\"" + attribute + "\"";
            } else {
                this.where += "\"" + attribute + "\"";
            }

            if (operator != null) {
                this.where += " " + operator + " ";
            } else {
                this.where += " = ";
            }
            this.where += "'" + value + "'";
            if (type != null) {
                this.where += type;
            }
        }
    }
    
    /**
     * @action ajoute la condition where (or) si la valeur à tester n'est pas nulle
     * @param attribute le nom de l'attribut qui sera testé
     * @param value la valeur de l'attribut à tester
     * @param operator l'opérateur de comparaison
     * @param type
     * @param tableAlias
     * @author Morgane Vidal, le 31 août 2017
     */
    public void appendORWhereConditionIfNeeded(String attribute, String value, String operator, String  type, String tableAlias) {
        if (value != null) {
            appendORWhereConditions(attribute, value, operator, type, tableAlias);
        }
    }
    /**
     * @action ajoute la condition where (and) si la valeur à tester n'est pas nulle
     * @param attribute le nom de l'attribut qui sera testé
     * @param value la valeur de l'attribut à tester
     * @param operator l'opérateur de comparaison
     * @param type
     * @param tableAlias 
     * @author Morgane Vidal, le 21 février 2017
     *
     */
    public void appendANDWhereConditionIfNeeded(String attribute, String value, String operator, String  type, String tableAlias) {
        if (value != null) {
            appendANDWhereConditions(attribute, value, operator, type, tableAlias);
        }
    }
    
    /**
     * ajoute un limit à la requête
     * @author Morgane Vidal, 21 février 2017
     * @param limit valeur de la limitation
     */
    public void appendLimit(String limit) {
        this.limit += limit;
    }
    
    public void appendOffset(String offset) {
        this.offset += offset;
    }
    
    public void appendANDWhereTableConstraintConditions(String attribute, String secondAttribute, String operator, String tableAlias, String secondTableAlias) {
        if (attribute != null && secondAttribute != null) {
            if (where.length() > 0) {
                this.where += " AND ";
            }
            if (tableAlias != null) {
                this.where += tableAlias + "." + "\"" + attribute + "\"";
            } else {
                this.where += "\"" + attribute + "\"";
            }

            if (operator != null) {
                this.where += " " + operator + " ";
            } else {
                this.where += " = ";
            }
            if (secondTableAlias != null) {
                this.where += secondTableAlias + "." + "\"" + secondAttribute + "\"";
            } else {
                this.where += "\"" + secondAttribute + "\"";
            }
        }
    }

    private void appendPrivateWhereConditions(String attribute, String value, String operator, String type, String tableAlias) {
        if (attribute != null && value != null) {
            if (tableAlias != null) {
                this.where += tableAlias + "." + "\"" + attribute + "\"";
            } else {
                this.where += "\"" + attribute + "\"";
            }

            if (operator != null) {
                this.where += " " + operator + " ";
            } else {
                this.where += " = ";
            }
            this.where += "'" + value + "'";
            if (type != null) {
                this.where += type;
            }
        }
    }

    @Override
    public String toString() {
        String query = "";
        if (attributes != null && attributes.length() > 0) {
            if (count) {
                if (distinct) {
                    query += "SELECT DISTINCT count(" + attributes + ") ";
                } else {
                    query += "SELECT count(" + attributes + ") ";
                }
            } else if (distinct) {
                query += "SELECT DISTINCT " + attributes + " ";
            } else {
                query += "SELECT " + attributes + " ";
            }
        } else if (count) {
            if (distinct) {
                query += "SELECT DISTINCT count(*) ";
            } else {
                query += "SELECT count(*) ";
            }

        } else if (distinct) {
            query += "SELECT DISTINCT * ";
        } else {
            query += "SELECT * ";
        }
        if (from != null) {
            query += from;
        }
        if (join != null) {
            query += join;
            query += "\n";
        }
        if (where != null && where.length() > 0) {
            query += "WHERE " + where;
        }
        if (orderBy != null && orderBy.length() > 0) {
            query += "\n";
            query += "ORDER BY ";
            query += orderBy;
        }
        if (limit != null && limit.length() > 0) {
            query += "\n";
            query += "LIMIT ";
            query += limit;
        }
        if (offset != null && offset.length() > 0) {
            query += "\n";
            query += "OFFSET ";
            query += offset;
        }
        return query;
    }
}
