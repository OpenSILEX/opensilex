package org.opensilex.sparql.model;

import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPARQLMultiLabels {
    private Map<String, List<String>> translationsOfAltLabels;

    public SPARQLMultiLabels() {
    }

    public SPARQLMultiLabels(Map<String, List<String>> translationsOfAltLabels) {
        this.translationsOfAltLabels = translationsOfAltLabels;
    }

    public Map<String, List<String>> getTranslationsOfAltLabels() {
        return translationsOfAltLabels;
    }

    public void setTranslationsOfAltLabels(Map<String, List<String>> translationsOfAltLabels) {
        this.translationsOfAltLabels = translationsOfAltLabels;
    }

    public Map<String, List<String>> getAllAltLabelsTranslations() {
        Map<String, List<String>> allTranslations = new HashMap<>();
        allTranslations.putAll(translationsOfAltLabels);

        return allTranslations;
    }

    public void addAllTranslations(Map<String, List<String>> labelTranslations) {

        translationsOfAltLabels.putAll(labelTranslations);
    }


    //    public void addTranslation(String value, String lang) {
//        translations.put(lang, value);
//    }
//

//    public SPARQLMultiLabels() {
//        translations = new HashMap<>();
//    }
//
//    public SPARQLMultiLabels(SPARQLMultiLabels labels) {
//
//        if(!MapUtils.isEmpty(labels.getAllTranslations())){
//            this.translations = new HashMap<>(labels.getAllTranslations());
//        }else{
//            this.translations = new HashMap<>();
//        }
//    }
//
//    public Map<String, String> getTranslations() {
//        return translations;
//    }
//
//    public void setTranslations(Map<String, String> translations) {
//        this.translations = translations;
//    }
//
//    public Map<String, String> getAllTranslations() {
//        Map<String, String> allTranslations = new HashMap<>();
//        allTranslations.putAll(translations);
//        return allTranslations;
//    }
//


//
//
//    public static SPARQLLabel fromMap(Map<String, String> translationMap) {
//        SPARQLLabel label = new SPARQLLabel();
//        label.addAllTranslations(translationMap);
//
//        return label;
//    }
}
