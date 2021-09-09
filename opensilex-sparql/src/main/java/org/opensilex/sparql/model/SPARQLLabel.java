/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vmigot
 */
public class SPARQLLabel {

    private String defaultValue;

    private String defaultLang;

    private Map<String, String> translations;

    public SPARQLLabel() {
        translations = new HashMap<>();
    }

    public SPARQLLabel(String value, String lang) {
        this();
        setDefaultValue(value);
        setDefaultLang(lang);
    }

    public SPARQLLabel(SPARQLLabel label) {
        this.defaultValue = label.getDefaultValue();
        this.defaultLang = label.getDefaultLang();
        if(!MapUtils.isEmpty(label.getTranslations())){
            this.translations = new HashMap<>(label.getTranslations());
        }
    }


    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public void setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
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
        if (defaultLang != null && defaultValue != null) {
            allTranslations.put(defaultLang, defaultValue);
        }
        return allTranslations;
    }

    public void addTranslation(String value, String lang) {
        translations.put(lang, value);
    }

    public void addAllTranslations(Map<String, String> labelTranslations) {
        translations.putAll(labelTranslations);
    }

    @Override
    public String toString() {
        return defaultValue;
    }

    public static SPARQLLabel fromMap(Map<String, String> translationMap) {
        SPARQLLabel label = new SPARQLLabel();
        label.addAllTranslations(translationMap);

        return label;
    }

}
