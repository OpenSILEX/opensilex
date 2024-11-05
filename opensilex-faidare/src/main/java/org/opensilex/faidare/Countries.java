/*
 * *****************************************************************************
 *                         Countries.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 15/07/2024 14:02
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Countries {

    public static List<CountryMap> getCountriesList() throws IOException {
        InputStream inputStream = Countries.class.getClassLoader().getResourceAsStream("countries.json");

        // Check if the input stream is not null
        if (inputStream == null) {
            throw new FileNotFoundException("Resource 'countries.json' not found.");
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
                inputStream,
                new TypeReference<List<CountryMap>>() {}
        );
    }

    public static class CountryMap {
        private final String enCountryName;
        private final String frCountryName;
        private final String alpha2code;
        private final String alpha3code;
        private final int numericCode;

        @JsonCreator
        public CountryMap(
                @JsonProperty("EnCountryName") String enCountryName,
                @JsonProperty("FrCountryName") String frCountryName,
                @JsonProperty("Alpha-2 code") String alpha2code,
                @JsonProperty("Alpha-3 code") String alpha3code,
                @JsonProperty("Numeric") int numericCode
        ) {
            this.enCountryName = enCountryName;
            this.frCountryName = frCountryName;
            this.alpha2code = alpha2code;
            this.alpha3code = alpha3code;
            this.numericCode = numericCode;
        }

        public String getEnCountryName() {
            return enCountryName;
        }

        public String getFrCountryName() {
            return frCountryName;
        }

        public String getAlpha2code() {
            return alpha2code;
        }

        public String getAlpha3code() {
            return alpha3code;
        }

        public int getNumericCode() {
            return numericCode;
        }
    }
}
