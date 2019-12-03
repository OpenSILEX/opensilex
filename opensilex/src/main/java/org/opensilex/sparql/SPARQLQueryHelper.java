/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vidalmor
 */
public class SPARQLQueryHelper {

    private SPARQLQueryHelper() {
    }

    public static final Var typeDefVar = makeVar("__type");
    
    public static final <T> Var getUriFieldVar(Class<T> objectClass) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = SPARQLClassObjectMapper.getForClass(objectClass);
        return makeVar(mapper.getURIFieldName());
    };

    public static Expr regexFilter(String fieldName, String regexPattern) {
        return regexFilter(fieldName, regexPattern, null);
    }

    public static Expr regexFilter(String fieldName, String regexPattern, String regexFlag) {
        if (regexPattern == null || regexPattern.equals("")) {
            return null;
        }

        if (regexFlag == null) {
            regexFlag = "i";
        }

        ExprVar name = new ExprVar(fieldName);
        Expr filter = new E_Regex(name, regexPattern, regexFlag);

        return filter;
    }

    public static Expr or(Expr... expressions) {
        Expr parentExpr = null;

        for (Expr expr : expressions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalOr(parentExpr, expr);
                }
            }
        }

        return parentExpr;
    }

    public static Expr and(Expr... expressions) {
        Expr parentExpr = null;

        for (Expr expr : expressions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalAnd(parentExpr, expr);
                }
            }
        }

        return parentExpr;
    }
}
