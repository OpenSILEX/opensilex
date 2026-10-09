---
changelog: |-
  Updating a resource no longer removes the links that other resources have towards it. For example, a variable
  keeps its unit, method, characteristic and entity when one of them is updated.
ignore-changelog: false
---

- [ ] Relecture MR
- [x] Tests écrits et OK
- [ ] Documentation technique (non concerné)
- [ ] Specifications fonctionnelles validées ([see spec template](../../opensilex-doc/src/main/resources/specs/template/spec_template.md))
- [ ] Testé
- [x] Remplir l'entrée changelog ou la marquer comme ignorée
  ([comment ?](https://forge.inrae.fr/OpenSILEX/opensilex-dev-tools/-/blob/master/docs/workflow/conventions/mr_redaction.md?ref_type=heads#description))

# Contexte

Mettre à jour une ressource supprimait aussi les relations que **d'autres** ressources avaient vers elle. Cas
signalé : après la modification d'une unité, les variables qui l'utilisaient perdaient leur lien `hasUnit` et
n'affichaient plus d'unité.

Cause : à la mise à jour, `SPARQLService` supprime puis réécrit les triplets de la ressource. La requête de
suppression effaçait aussi tous les triplets où la ressource est **objet** (relations inverses), y compris ceux
qui appartiennent à d'autres ressources et que la mise à jour ne réécrit jamais.

## Avant

- **API** : `PUT /core/units` (de même pour les méthodes, caractéristiques et entités) supprime le lien des
  variables vers la ressource mise à jour.
- **Front** : après modification d'une unité, les variables concernées n'ont plus d'unité.

## Après

- **API** : la mise à jour ne supprime plus que les relations inverses déclarées dans le modèle de la ressource
  mise à jour ; les liens portés par les autres ressources sont conservés.
- **Front** : les variables gardent leur unité, méthode, caractéristique et entité après la modification de
  celles-ci.

# Changements

## Requête de suppression lors d'une mise à jour (`opensilex-sparql`)

- `SPARQLClassQueryBuilder.java` : la partie « relations inverses » de la requête de suppression est restreinte
  aux prédicats gérés par le modèle (`FILTER (?p IN (...))`). Les triplets dont la ressource est sujet restent
  tous supprimés puis réécrits, comme avant.
- `SPARQLService.java` : `getCustomRelationsForType(type, analyzer)` est extraite de `deleteCustomRelations` pour
  pouvoir calculer les relations personnalisées d'un type sans dupliquer ce code.

## Tests

- `VariableApiTest.java` : nouveau test `testVariableLinksUnchangedAfterLinkedModelUpdate`, qui met à jour
  l'unité, la méthode, la caractéristique et l'entité d'une variable puis vérifie qu'elle y est toujours liée.
- `UnitApiTest.java`, `MethodApiTest.java`, `CharacteristicApiTest.java` : ajout des descriptions du service de
  mise à jour utilisées par ce test.

# Autres

## Points d'attention pour la relecture

- Le changement concerne **toutes** les mises à jour SPARQL (`SPARQLService#update`), pas seulement les variables :
  une relation inverse qui n'est pas un champ du modèle n'est plus supprimée à la mise à jour. Si un modèle
  comptait sur cette suppression, il faut le déclarer comme champ.
- Commencer la lecture par `SPARQLClassQueryBuilder#getDeleteBuilder`.

## Comment tester

1. Créer une variable avec une unité.
2. Modifier le nom de l'unité.
3. Ouvrir la variable : l'unité est toujours affichée, avec son nouveau nom.
