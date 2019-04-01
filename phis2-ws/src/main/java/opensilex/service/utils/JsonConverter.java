package opensilex.service.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JsonConverter - Classe possédant deux méthodes qui permettent la convertion
 * d'objets en chaines de caractères au format JSON
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @date 03/12/2015
 */
@Singleton
public final class JsonConverter {

    /**
     * Récupération des erreurs
     */
    final static Logger LOGGER = LoggerFactory.getLogger(JsonConverter.class);

    /**
     * ConvertToPrettyJson() - Méthode de classe qui permet de convertir un
     * objet quelconque en une chaine de caractère au format JSON qui sera
     * lisible par un utilisateur
     *
     * @param o L'objet à convertir
     *
     * @return La chaine de caractère au format JSON
     * @date 03/12/2015
     */
    public static String ConvertToPrettyJson(Object o) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String jsonObject = gson.toJson(o);
        return jsonObject;
    }

    /**
     * ConvertToJson() - Méthode de classe qui permet de onvertir un objet
     * quelconque en une chaine de caractère au format JSON
     *
     * @param o l'objet à convertir
     *
     * @return La chaine de caractère au format JSON
     * @date 03/12/2015
     * @update 06/2016 AC Récupération des exceptions de sérialization
     */
    public static String ConvertToJson(Object o) {
        final Gson gson = new GsonBuilder().create();
        String jsonObject = null;
        try {
            jsonObject = gson.toJson(o);
        } catch (Exception e) {
//            e.printStackTrace();
            LOGGER.error(e.getMessage(), e);

        }

        return jsonObject;
    }

    /**
     * @author Arnaud Charleroy
     * @param j Chaîne à convertir
     * @param c Classe de l'objet dans lequel la chaîne doit être convertie
     * @return Objet du converti du JSON
     *
     */
    public static Object ConvertFromJson(String j, Class c) {
        final Gson gson = new GsonBuilder().create();
        Object jsonObject;
        try {
            jsonObject = c.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        jsonObject = gson.fromJson(j, c);

        return jsonObject;
    }

}
