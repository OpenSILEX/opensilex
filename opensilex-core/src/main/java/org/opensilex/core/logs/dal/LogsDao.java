///*
// * ******************************************************************************
// *                                     FactorDAO.java
// *  OpenSILEX
// *  Copyright © INRA 2019
// *  Creation date:  17 December, 2019
// *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// * ******************************************************************************
// */
//package org.opensilex.core.logs.dal;
//
//import org.opensilex.nosql.service.NoSQLService;
//
//
///**
// *
// * @author Arnaud Charleroy
// */
//public class LogsDao {
//
//    protected final NoSQLService nosql;
//
//    public LogsDao(NoSQLService nosql) {
//        this.nosql = nosql;
//    }
//
//    public LogModel create(LogModel instance) throws Exception {
//        nosql.create(instance);
//        return instance;
//    }
//
//    public LogModel update(LogModel instance) throws Exception {
//        nosql.update(instance);
//        return instance;
//    }
//
////    public void delete(LogModel instanceURI) throws Exception {
////        nosql.delete(instanceURI);
////    }
//
////    public LogModel get(URI instanceURI) throws Exception {
////        return nosql.findById(LogModel.class, instanceURI);
////    }
////            
////    public ListWithPagination<LogModel> search(String alias, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
////        return sparql.searchWithPagination(
////                LogModel.class,
////                null,
////                (SelectBuilder select) -> {
////                    // TODO implements filters
////                    appendFilters(alias, select);
////                },
////                orderByList,
////                page,
////                pageSize
////        );
////    }
////
////    /**
////     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for each non-empty simple attribute ( not a {@link List} from the {@link FactorSearchDTO}
////     *
////     * @param alias alias search attribute
////     * @param select search query
////     * @throws java.lang.Exception can throw an exception
////     * @see SPARQLQueryHelper the utility class used to build Expr
////     */
////    protected void appendFilters(String alias, SelectBuilder select) throws Exception {
////
////        List<Expr> exprList = new ArrayList<>();
////
////        // build regex filters
////        if (alias != null) {
////            exprList.add(SPARQLQueryHelper.regexFilter(LogModel.ALIAS_FIELD, alias));
////        }
////
////        for (Expr filterExpr : exprList) {
////            select.addFilter(filterExpr);
////        }
////    }
//}
