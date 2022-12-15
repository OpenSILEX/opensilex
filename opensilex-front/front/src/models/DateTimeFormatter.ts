import VueI18n from "vue-i18n";

/**
 * Argument type for all date formatting operations. This is the same type as the {@link Date} constructor.
 */
export type DateArg = number | string | Date;

/**
 * Mean number of days in a year
 */
const DAYS_IN_A_YEAR = 365.2425;
/**
 * Mean number of days in a month
 */
const DAYS_IN_A_MONTH = DAYS_IN_A_YEAR / 12;

/**
 * A utility class to format dates
 *
 * @author Valentin Rigolle
 */
export default class DateTimeFormatter {
    $i18n: VueI18n;

    constructor(i18n: VueI18n) {
        this.$i18n = i18n;
    }

    /**
     * Formats a date without the time (YYYY-MM-DD)
     *
     * @param dateValue
     */
    formatISODate(dateValue: DateArg): string {
        if (!dateValue) {
            return "";
        }
        return new Date(dateValue).toISOString().substring(0, 10);
    }

    /**
     * Returns only the time part of a date (HH:mm:ss)
     *
     * @param dateTimeValue
     */
    formatISOTime(dateTimeValue: DateArg): string {
        if (!dateTimeValue) {
            return "";
        }
        return new Date(dateTimeValue).toISOString().substring(11, 19);
    }

    /**
     * Formats a date with the time, without the timezone (YYYY-MM-DDTHH:mm:ss)
     *
     * @param dateTimeValue
     */
    formatISODateTime(dateTimeValue: DateArg): string {
        if (!dateTimeValue) {
            return "";
        }
        return new Date(dateTimeValue).toISOString().substring(0, 19);
    }

    /**
     * Formats a date in the current locale (which depends on the language the user chooses).
     *
     * @param dateTimeValue
     */
    formatLocaleDate(dateTimeValue: DateArg): string {
        return new Intl.DateTimeFormat(this.$i18n.t("dateTimeLocale").toString(), {
            dateStyle: 'short'
        } as Intl.DateTimeFormatOptions).format(new Date(dateTimeValue));
    }

    /**
     * Formats a date with the time and the timezone in the current locale (which depends on the language the user chooses).
     *
     * @param dateTimeValue
     */
    formatLocaleDateTime(dateTimeValue: DateArg): string {
        return new Intl.DateTimeFormat(this.$i18n.t("dateTimeLocale").toString(), {
            dateStyle: 'short',
            timeStyle: 'long'
        } as Intl.DateTimeFormatOptions).format(new Date(dateTimeValue));
    }

    /**
     * Formats a period between two dates
     *
     * @param startDateValue
     * @param endDateValue
     */
    formatPeriod(startDateValue: DateArg, endDateValue: DateArg): string {
        let startDate = new Date(startDateValue);
        let endDate: Date;
        let result = this.formatISODate(startDateValue);

        if (endDateValue) {
            endDate = new Date(endDateValue);
            result += " - " + this.formatISODate(endDateValue);
        } else {
            endDate = new Date();
        }

        let diffDays = Math.floor((endDate.getTime() - startDate.getTime()) / 1000 / 3600 / 24);
        const years = Math.floor(diffDays / DAYS_IN_A_YEAR);
        diffDays %= DAYS_IN_A_YEAR;
        const months = Math.floor(diffDays / DAYS_IN_A_MONTH);
        diffDays %= DAYS_IN_A_MONTH;
        const days = Math.floor(diffDays);

        let periodStrings: Array<string> = [];
        if (years > 0) {
            if (years === 1) {
                periodStrings.push(years + " " + this.$i18n.t("component.common.year").toString());
            }
            if (years > 1) {
                periodStrings.push(years + " " + this.$i18n.t("component.common.years").toString());
            }
        }

        if (months > 0) {
            if (months === 1) {
                periodStrings.push(months + " " + this.$i18n.t("component.common.month").toString());
            }
            if (months > 1) {
                periodStrings.push(months + " " + this.$i18n.t("component.common.months").toString());
            }
        }

        if (days > 0) {
            if (days === 1) {
                periodStrings.push(days + " " + this.$i18n.t("component.common.day").toString());
            }
            if (days > 1) {
                periodStrings.push(days + " " + this.$i18n.t("component.common.days").toString());
            }
        }

        if (periodStrings.length > 0) {
            result += " (" + periodStrings.join(", ") + ")";
        }

        return result;
    }
}

