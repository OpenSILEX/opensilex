// //******************************************************************************
// //                          ExperimentDAO.java
// // OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// // Copyright © INRAE 2020
// // Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// //******************************************************************************
// package org.opensilex.siduri.experimentFdF.dal;

// import org.opensilex.core.experiment.dal.ExperimentDAO;
// import java.net.URI;
// import java.time.LocalDate;
// import org.opensilex.utils.ListWithPagination;
// import org.opensilex.sparql.service.SPARQLQueryHelper;
// import org.opensilex.sparql.service.SPARQLService;
// /**
//  * @author Vincent MIGOT
//  * @author Renaud COLIN
//  */
// public class ExperimentFdFDAO extends ExperimentDAO {

//     @Override
//     public ListWithPagination<ExperimentFdFModel> search(ExperimentFdFSearchFilter filter) throws Exception {
//         LocalDate startDate;
//         LocalDate endDate;
//         if (filter.getYear() != null) {
//             String yearString = Integer.toString(filter.getYear());
//             startDate = LocalDate.of(filter.getYear(), 1, 1);
//             endDate = LocalDate.of(filter.getYear(), 12, 31);
//         } else {
//             startDate = null;
//             endDate = null;
//         }

//         ListWithPagination<ExperimentFdFModel> xps = sparql.searchWithPagination(
//                 ExperimentModel.class,
//                 null,
//                 (SelectBuilder select) -> {
//                     appendRegexLabelFilter(select, filter.getName());
//                     appendSpeciesFilter(select, filter.getSpecies());
//                     appendFactorFilter(select, filter.getFactorCategories());
//                     appendIsActiveFilter(select, filter.isEnded());
//                     appendDateFilter(select, startDate, endDate);
//                     appendProjectListFilter(select, filter.getProjects());
//                     appendUserExperimentsFilter(select, filter.getUser());
//                     appendPublicFilter(select, filter.isPublic());
//                     appendFacilitiesFilter(select, filter.getFacilities());
//                     appendFdFFilter(select, filter.isFdF() );
//                 },
//                 filter.getOrderByList(),
//                 filter.getPage(),
//                 filter.getPageSize()
//         );

//         return xps;

//     }

//     private void appendFdFFilter(SelectBuilder select, Boolean FdF) throws Exception {
//         if (FdF != null) {
//             select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.IS_FDF_FIELD, FdF));
//         }
//     }

// }
