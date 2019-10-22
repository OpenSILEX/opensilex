$(document).ready(function () {
    $("#ws-build-version").text("${ws.version}");
    $("#ws-name").text("Swagger UI - " + "${ws.target}");
});
/**
 * Récupère l'adresse et le port utilises pour voir la documentation du webservice
 * @returns Array
 */
function getConfig(){
    return {
        "serviceURLName": "${ws.target}",
        "resourcesURLName": "${ws.baseUrl}"
    };
}
/**
 * Fonction utilisee pour rediger l'utilisateur lorsqu'il clique sur le lien vers la documentation
 * @returns {Boolean}
 */
function getDocumentation() {
    var config = getConfig();
    var apiUrl =  window.location.protocol + "//" + window.location.host + "/"+ config.serviceURLName +"/api-docs";
    window.location = apiUrl;
    return false;
}
/**
 * Fonction utilisée pour rediger l'utilisateur lorsqu'il clique sur le lien vers le fichier de configuration sawagger généré
 * @returns {Boolean}
 */
function getSwaggerLink() {
   var config = getConfig();
   var apiUrl =  window.location.protocol + "//" + window.location.host + "/" + config.serviceURLName + "/" + config.resourcesURLName + "/swagger.json";
   window.location = apiUrl;
   return false;
}