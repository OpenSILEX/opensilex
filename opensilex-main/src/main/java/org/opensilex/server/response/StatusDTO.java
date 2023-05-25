//******************************************************************************
//                          StatusDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.Collections;
import java.util.Map;

/**
 * <pre>
 * Response metadata status DTO.
 *
 * A status is composed of a message and a messageType which could be: - ERROR -
 * WARN - INFO - DEBUG
 * </pre>
 *
 * @see org.opensilex.server.response.MetadataDTO
 * @author Vincent Migot
 */
public class StatusDTO {

    /**
     * Status message.
     */
    private final String message;

    /**
     * Translation key, if status message need to be translated.
     */
    private final String translationKey;

    /**
     * Translation values if the translation key takes parameters.
     */
    private final Map<String, String> translationValues;



    /**
     * Status message type ERROR|WARN|INFO|DEBUG.
     */
    public final StatusLevel level;

    public StatusDTO(String message, StatusLevel level) {
        this(message, level, null);
    }

    public StatusDTO(String message, StatusLevel level, String translationKey) {
        this(message, level, translationKey, Collections.emptyMap());
    }

    public StatusDTO(String message, StatusLevel level, String translationKey, Map<String, String> translationValues) {
        this.message = message;
        this.level = level;
        this.translationKey = translationKey;
        this.translationValues = translationValues;
    }

    /**
     * Getter for message.
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Getter for status message type.
     *
     * @return message type
     */
    public StatusLevel getLevel() {
        return level;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Map<String, String> getTranslationValues() {
        return translationValues;
    }
}
