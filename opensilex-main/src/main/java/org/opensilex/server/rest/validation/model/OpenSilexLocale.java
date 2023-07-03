package org.opensilex.server.rest.validation.model;

import org.opensilex.server.exceptions.BadRequestException;

import java.util.*;

/**
 * Object that validates string representations of languages. The {@link Locale} class doesn't natively do it.
 *
 * @author Gabriel Besombes
 */
public class OpenSilexLocale {
    private static final Set<String> ISO_LANGUAGES = new HashSet<>(Arrays.asList(Locale.getISOLanguages()));

    private final Locale locale;

    public Locale getLocale() {
        return locale;
    }

    public static Optional<Locale> getLocaleFromString(String localeString) {
        if (ISO_LANGUAGES.contains(localeString)) {
            return Optional.ofNullable(Locale.forLanguageTag(localeString));
        }
        return Optional.empty();
    }

    public OpenSilexLocale(String localeString) {
        this.locale = getLocaleFromString(localeString)
                .orElseThrow(() -> new BadRequestException("String '" + localeString + "' couldn't be matched with a known language"));
    }

    public String getDisplayLanguage(Locale locale) {
        return this.locale.getDisplayLanguage(locale);
    }
    @Override
    public String toString() {return locale.toString();}
    public String toLanguageTag() {return locale.toLanguageTag();}
}
