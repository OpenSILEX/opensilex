//******************************************************************************
//                              ResourcesUtils.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Resource useful functions.
 * @author Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResourcesUtils {

    /**
     * Generates a unique string of 32 characters.
     * @return the UUID
     */
    public static String getUniqueID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Returns the boolean value of the given string.
     * @param bool String
     * @return true if the string is equals to "true" or "t" (it is not case
     * sensitive) false if the string is not equals to the precedent strings
     */
    public static boolean getStringBooleanValue(String bool) {
        return (bool.equalsIgnoreCase("true") || bool.equalsIgnoreCase("t"));
    }

    /**
     * Capitalizes the first Letter of the string.
     * @param original the string for which we wants the first letter to be
     * capitalized
     * @return the given string with the first letter capitalized
     */
    public static String capitalizeFirstLetter(String original) {
        return (original == null || original.length() == 0) ? original : original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }

    /**
     * Splits a string using the given separator.
     * @param values
     * @param separator
     * @return the list of elements separated by the given separator in the
     * string values
     */
    public static List<String> splitStringWithGivenPattern(String values, String separator) {
        List<String> listValues;
        try {
            listValues = Arrays.asList(values.split(separator));
        } catch (Exception e) {
            listValues = new ArrayList<>();
            listValues.add(values);
        }
        return listValues;
    }

    /**
     * ExtentY to plantHeight width to plantWidth.
     * @param mongoUserVariable
     * @return
     */
    public static String formatMongoImageAnalysisVariableForDB(String mongoUserVariable) {
        String tmpMongoUserVariable;
        String variableFormatMongo = "";
        // elcom special case
        if (mongoUserVariable.contains("parallelBoudingBox")) {
            tmpMongoUserVariable = mongoUserVariable.replaceFirst("parallelBoudingBox", "");
            variableFormatMongo = "parallelBoudingBox_" + tmpMongoUserVariable.toLowerCase();
        } else if (mongoUserVariable.contains("nonParallelBoudingBox")) {
            tmpMongoUserVariable = mongoUserVariable.replaceFirst("nonParallelBoudingBox", "");
            variableFormatMongo = "nonParallelBoudingBox" + tmpMongoUserVariable.toLowerCase();
        } else {
            for (int i = 0; i < mongoUserVariable.length(); i++) {
                if (Character.isUpperCase(mongoUserVariable.charAt(i))) {
                    variableFormatMongo += "_";
                    variableFormatMongo += Character.toLowerCase(mongoUserVariable.charAt(i));
                } else {
                    variableFormatMongo += mongoUserVariable.charAt(i);
                }
            }
        }
        return variableFormatMongo;
    }

    /**
     * Gets the value of a URI.
     * @param uri
     * @return
     */
    public static String getValueOfURI(String uri) {
        final String[] parts = uri.split("#");
        if ((uri.contains(">") && uri.contains("<"))) {
            return parts[1].substring(0, parts[1].length() - 1);
        } else {
            return parts[1];
        }
    }

    /**
     * Generates a new agent URI suffix.
     * @param firstName .e.g Arnaud
     * @param familiyName .e.g Charleroy
     * @return the agent suffix
     * @example arnaud_charleroy
     */
    public static String createUserUriSuffix(String firstName, String familiyName) {
        String trimmedFirstName = firstName.trim();
        String trimmedFamilyName = familiyName.trim();
        String checkedFamilyName = trimmedFamilyName.replace(" ", "-");
        String checkedFirstName = trimmedFirstName.replace(" ", "-");
        return checkedFirstName.toLowerCase() + "_" + checkedFamilyName.toLowerCase();
    }
}
