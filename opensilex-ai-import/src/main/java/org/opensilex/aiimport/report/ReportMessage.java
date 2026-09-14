//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sentence the report produces, said twice: once as a translation key for the interface, once in
 * English for the language model.
 * <p>
 * Both are needed, and neither can replace the other. The interface is French for a French user, so
 * it needs a key and its parameters. The same sentence also goes into the system prompt, where
 * English is the right choice and a translation key would be meaningless — the model cannot resolve
 * {@code AiImport.report.hint.experimentMissing}, and the prompt already tells it to answer in the
 * user's language.
 * <p>
 * Keeping the English text here rather than deriving it from a key also keeps the two honest: the
 * sentence a reviewer reads in the Java is the sentence the model receives.
 *
 * @author Arnaud Charleroy
 */
public class ReportMessage {

    private final String key;
    private final String english;
    private final Map<String, String> params = new LinkedHashMap<>();

    private ReportMessage(String key, String english) {
        this.key = key;
        this.english = english;
    }

    /**
     * @param key     the translation key, under {@code AiImport.report.}
     * @param english the same sentence in English, for the prompt
     */
    public static ReportMessage of(String key, String english) {
        return new ReportMessage(key, english);
    }

    /**
     * A sentence with no translation yet — a profile's own wording, or a value quoted back. It
     * still reaches the interface, in English, rather than being dropped.
     */
    public static ReportMessage plain(String english) {
        return new ReportMessage(null, english);
    }

    public ReportMessage with(String name, Object value) {
        params.put(name, String.valueOf(value));
        return this;
    }

    /**
     * The English text of each message, which is what a prompt carries.
     */
    public static List<String> english(List<ReportMessage> messages) {
        List<String> texts = new ArrayList<>(messages.size());
        messages.forEach(message -> texts.add(message.getEnglish()));
        return texts;
    }

    /**
     * @return true when the list already says this, compared on the English text — the same
     *         sentence twice is still one sentence
     */
    public static boolean alreadySaid(List<ReportMessage> messages, ReportMessage candidate) {
        return messages.stream()
                .anyMatch(existing -> existing.getEnglish().equals(candidate.getEnglish()));
    }

    public String getKey() {
        return key;
    }

    public String getEnglish() {
        return english;
    }

    public Map<String, String> getParams() {
        return params;
    }

    /**
     * What the model reads. The English sentence, so a report summary in a prompt says something.
     */
    @Override
    public String toString() {
        return english;
    }
}
