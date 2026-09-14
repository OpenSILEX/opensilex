//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.text.Normalizer;
import java.util.List;

/**
 * Locates a column by the words its header contains, ignoring case, accents and separators.
 * <p>
 * Field templates version their header names, so a catalogue column can be called
 * {@code Nouvelle_Abréviation_FR_maj_03_2023} in one revision and something else in the next.
 * Matching on words survives that; matching on an exact name does not.
 *
 * @author Arnaud Charleroy
 */
public class HeaderMatcher {

    private HeaderMatcher() {
    }

    /**
     * @param headers the header row
     * @param words   words that must all appear in the header, already lowercase and unaccented
     * @return the first matching header as written in the file, or {@code null}
     */
    public static String find(List<String> headers, String... words) {
        for (String header : headers) {
            String normalized = normalize(header);
            boolean allPresent = true;
            for (String word : words) {
                if (!normalized.contains(word)) {
                    allPresent = false;
                    break;
                }
            }
            if (allPresent) {
                return header;
            }
        }
        return null;
    }

    /**
     * @return the header lowercased, stripped of accents, and with separators removed
     */
    public static String normalize(String header) {
        if (header == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(header, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return decomposed.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
