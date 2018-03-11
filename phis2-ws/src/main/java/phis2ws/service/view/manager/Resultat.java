//**********************************************************************************************
//                                       Resultat.java 
//
// Author(s): Samuël Chérimont, Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: december 2015
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: A class which represente result part in response form 
//***********************************************************************************************
package phis2ws.service.view.manager;

import phis2ws.service.view.brapi.Pagination;
import java.util.ArrayList;

/**
 * Resultat - Classe abstraite offrant un modèle pour la présentation des
 * résultats sous forme d'un objet qui sera conforme aux standarts de la web api
 * au format JSON
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @param <T> Classe des objets conetnus dans la liste data
 *
 * @see Pagination
 * @date 14/12/2015
 */
public abstract class Resultat<T> {

    ArrayList<T> data;

    public Resultat() {
        data = new ArrayList<>();
    }

    
    
    /**
     * Resultat() - Constructeur appelé dans le cas ou un seul résultat est
     * demandé (Pagination non nécessaire)
     *
     * @param list Liste contenant un seul objet ou zero
     * @date 14/12/2015
     */
    public Resultat(ArrayList<T> list) {
        this.data = new ArrayList();
        if(list.size() > 0){
            data.add(list.get(0));
        }
        
    }

    /**
     * Resultat() - Constructeur qui prend en compte la pagination
     *
     * @param list Liste contenant plusieurs éléments
     * @param pagination Objet Pagination permettant de trier la liste list
     * @param paginate
     *
     * @see copyList()
     * @date 14/12/2015
     */
//    @SuppressWarnings("OverridableMethodCallInConstructor")
    public Resultat(ArrayList<T> list, Pagination pagination, boolean paginate) {
        if(paginate){
            this.data = list;
        }else{
            this.data = copyList(list, pagination);
        }
       
    }
    

    /**
     * copyList() - Trie une liste d'éléments selon la page demandée et le
     * nombre de résultats par page
     *
     * @param list liste des éléments non triés
     * @param pagination Objet contenant les données de pagination nécessaires
     * pour le tri de la liste list
     * @return la liste des éléments paginée
     * @date 14/12/2015
     * @Update AC 08/16 première page = page 0 BRAPI
     */
     final ArrayList<T> copyList(ArrayList<T> list, Pagination pagination) {
        ArrayList<T> finalList = new ArrayList();
        if (pagination.getCurrentPage() > pagination.getTotalPages()) {
            return finalList;
        }
//        int i = (pagination.getCurrentPage() - 1) * pagination.getPageSize();
// Modification BRAPI page 0 au lieu de page 1 dans metadata
        int i = (pagination.getCurrentPage()) * pagination.getPageSize();
        int tmp = i;
        while (i < list.size() && (i - tmp) < pagination.getPageSize()) {
            finalList.add(list.get(i));
            i++;
        }
        return finalList;
    };

    /**
     * dataSize() - Recupère le nombre d'objets du champ data
     *
     * @return Le nombre d'objets de la liste data ou 0 si la liste n'a pas été
     * initialisée
     * @date 14/12/2015
     */
    public int dataSize() {
        if (data != null) {
            return data.size();
        } else {
            return 0;
        }
    }

    public ArrayList<T> getData() {
        return data;
    }

    public void setData(ArrayList<T> data) {
        this.data = data;
    }
    
}
