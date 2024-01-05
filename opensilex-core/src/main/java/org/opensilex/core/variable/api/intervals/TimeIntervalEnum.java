package org.opensilex.core.variable.api.intervals;

import java.util.Locale;
import java.util.ResourceBundle;

public enum TimeIntervalEnum {

    MILISECOND("milisecond"),
    SECOND("second"),
    MINUTES("minutes"),
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    UNIQUE_MEASUREMENT("unique_measurement");

    private final String labelKey;

    private TimeIntervalEnum(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabel(Locale locale) {
        return ResourceBundle.getBundle("intervalsLabel", locale).getString(labelKey);
    }
}
