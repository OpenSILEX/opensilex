package org.opensilex.server.rest.validation.model;

import org.opensilex.server.exceptions.BadRequestException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Object that validates string representations of languages. The {@link Locale} class doesn't natively do it.
 *
 * @author Gabriel Besombes
 */
public class OpenSilexLocale {
    private static final Set<String> ISO_LANGUAGES = new HashSet<>(Arrays.asList(Locale.getISOLanguages()));

    private static final List<Locale> SUPPORTED_LANGUAGES = ISO_LANGUAGES.stream()
            .map(lang -> new Locale(lang, "")).collect(Collectors.toList());

    private static final Map<String, Locale> DISPLAY_COUNTRIES = makeDisplayCountriesMap();

    private static Map<String, Locale> makeDisplayCountriesMap(){
        Map<String, Locale> displayCountriesMap = new HashMap<>();
        for (Locale language : SUPPORTED_LANGUAGES) {
            displayCountriesMap.putAll(
                    Arrays.stream(Locale.getAvailableLocales())
                            .distinct()
                            .collect(Collectors.toMap(
                                    locale -> locale.getDisplayCountry(language).toLowerCase(),
                                    locale -> locale,
                                    (r1, r2) -> r1
                            ))
            );
        }
        return displayCountriesMap;
    }

    private final Locale locale;

    public Locale getLocale() {
        return locale;
    }

    public static Optional<Locale> getLocaleFromLanguageString(String localeString) {
        if (ISO_LANGUAGES.contains(localeString)) {
            return Optional.ofNullable(Locale.forLanguageTag(localeString));
        }
        return Optional.empty();
    }

    public static Optional<Locale> getLocaleFromCountryString(String localeString) {
        if (DISPLAY_COUNTRIES.containsKey(localeString.toLowerCase())) {
            return Optional.ofNullable(DISPLAY_COUNTRIES.get(localeString.toLowerCase()));
        }
        return Optional.empty();
    }

    public OpenSilexLocale(String localeString) {
        this.locale = getLocaleFromLanguageString(localeString)
                .orElseGet(() -> getLocaleFromCountryString(localeString)
                        .orElseThrow(() -> new BadRequestException(
                                "String '" + localeString + "' couldn't be matched with a known language or country"
                        )));
    }

    public String getDisplayLanguage(Locale locale) {
        return this.locale.getDisplayLanguage(locale);
    }

    @Override
    public String toString() {return locale.toString();}

    public String toLanguageTag() {return locale.toLanguageTag();}

    public String getISO3Country() {return this.locale.getISO3Country();}
}
