# Recherche Data Gouv - Aide Dataverse

Auteur: Gabriel Besombes - Opensilex (01/06/2023)

## Informations générales sur Recherche Data Gouv

Selon <a class="external-url-link" target="_blank" rel="noopener noreferrer" href="https://entrepot.recherche.data.gouv.fr/">le site web de Recherche Data Gouv</a> :
L’entrepôt pluridisciplinaire _Recherche Data Gouv_ est une solution souveraine pour le partage et l’ouverture des données de recherche produites par les communautés qui ne disposent pas d’un entrepôt disciplinaire reconnu. Il est basé sur le logiciel libre <a class="external-url-link" target="_blank" rel="noopener noreferrer" href="https://guides.dataverse.org/en/latest/">Dataverse</a>. Le dépôt des données doit se faire dans l’espace institutionnel attribué à l’établissement dont relève un des contributeurs. Un espace générique est dédié aux données produites par les établissements ne disposant pas d’espace dédié. Les tests sont à effectuer dans <a class="external-url-link" target="_blank" rel="noopener noreferrer" href="https://demo.recherche.data.gouv.fr/">le bac à sable</a>.
Retrouver les actualités et événements de la <a class="external-url-link" target="_blank" rel="noopener noreferrer" href="https://recherche.data.gouv.fr/en">plateforme Recherche Data Gouv</a>.

__Note :__ Si la sandbox de Recherche Data Gouv est utilisée pour votre instance, vos données seront supprimées tous les mois du côté de Recherche Data Gouv. Vous pouvez demander à votre administrateur si c'est le cas.
## A propos de l'intégration d'OpenSilex avec Recherche Data Gouv

Cette version est un prototype et encore en cours de développement. C'est pourquoi elle a certaines limites :
* L'intégration est à sens unique. Elle permet la création de brouillons de _Dataset_ sur Recherche Data Gouv à partir de métadonnées présentes sur OpenSILEX.
* Une fois la création du brouillon de _Dataset_ est réalisée sur OpenSILEX vous devez vous rendre sur le site de Recherche Data Gouv pour vérifier les métadonnées, les compléter, ajouter les données et enfin publier le dataset.
* Si le brouillon de _Dataset_ est supprimé sur Recherche Data gouv il ne sera pas automatiquement supprimé sur votre instance OpenSILEX.
