import VueI18n from "vue-i18n";

/**
 * A utility class to format numbers
 *
 * @author Sebastien Prado
 */
export default class NumberFormatter {
    $i18n: VueI18n;

    constructor(i18n: VueI18n) {
        this.$i18n = i18n;
    }

    /**
     * Returns formatted numbers and legends for elements counting
     *
     * @param element
     */
    formateResponse(element): string {
        if (element >= 1 && element < 1000) {
            return element;
        }
        // rounded down to the nearest thousand : 12 552 : 12K+
        if (element >= 1000 && element < 1000000) {
            return Math.floor(element / 1000) + "K+";
        }
        // rounded down to the nearest million: 125 265 342 : 125M+
        if (element >= 1000000) {
            return Math.floor(element / 1000000) + "M+"
        }
      }
}

