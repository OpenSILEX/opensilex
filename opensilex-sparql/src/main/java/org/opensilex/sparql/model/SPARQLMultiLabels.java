package org.opensilex.sparql.model;

import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

public class SPARQLMultiLabels {
    private Map<String, String> translations;

    public SPARQLMultiLabels() {
        translations = new HashMap<>();
    }

    public SPARQLMultiLabels(SPARQLMultiLabels labels) {

        if(!MapUtils.isEmpty(labels.getAllTranslations())){
            this.translations = new HashMap<>(labels.getAllTranslations());
        }else{
            this.translations = new HashMap<>();
        }
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public Map<String, String> getAllTranslations() {
        Map<String, String> allTranslations = new HashMap<>();
        allTranslations.putAll(translations);
        return allTranslations;
    }

    public void addTranslation(String value, String lang) {
        translations.put(lang, value);
    }

    public void addAllTranslations(Map<String, String> labelTranslations) {
        translations.putAll(labelTranslations);
    }


    public static SPARQLLabel fromMap(Map<String, String> translationMap) {
        SPARQLLabel label = new SPARQLLabel();
        label.addAllTranslations(translationMap);

        return label;
    }
}
