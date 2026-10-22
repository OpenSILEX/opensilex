# Revue d'un composant Vue OpenSILEX

## Déroulé

1. **Délimiter le périmètre.** Les fichiers désignés par l'utilisateur, sinon le diff : `git diff --name-only <base>...HEAD -- '*.vue' '*.ts'` (base habituelle : `vue3/main`).
2. **Passer le contrôle mécanique** sur ces fichiers :
   ```bash
   .claude/skills/opensilex-vue/scripts/check-vue-conventions.sh <fichiers.vue>
   ```
   Il signale l'ordre des blocs, `<script setup lang="ts">`, `<style scoped lang="scss">`, la syntaxe de `defineEmits`, les refs de gabarit, `any`, les vestiges Vue 2, les classes Bootstrap, les régions et la place de `defineExpose`. Ses `ERROR` sont des violations certaines. Ses `WARN` sont à examiner : un `ref<InstanceType<…>>` peut être une ref de données légitime, mais c'est rare.
3. **Faire la revue manuelle**, avec la grille ci-dessous. C'est elle qui trouve les vrais problèmes : le script ne lit pas le sens du code.
4. **Pour une migration**, comparer avec l'original Vue 2 : `git show origin/develop:<chemin>` (la branche `develop` porte encore le code Vue 2). Chaque filtre, colonne, action et événement de l'ancien composant est soit porté, soit supprimé **volontairement** comme code mort, et la revue dit lequel.
5. **Rendre le rapport** (format en bas de page).

## Grille manuelle

### Structure

- [ ] Blocs dans l'ordre `<template>` → `<script setup lang="ts">` → `<style scoped lang="scss">` → `<i18n>` ; pas de bloc vide.
- [ ] Région `Public` : `defineEmits`, `defineProps`, `defineModel`, dans cet ordre.
- [ ] Région `Private` avec ses sous-régions dans l'ordre : Plugins & services → Template refs → Data & Computed → Hooks/watchers → Event handlers → Methods. Un petit fichier peut se limiter à Public / Private.
- [ ] `defineExpose()` en dernier, et n'expose que ce que le parent appelle réellement (`graft grep` sur le nom de la ref côté parent).
- [ ] Gestionnaires nommés `on<Événement>` ou `on<Enfant><Événement>`, payload typé, dans la région `Event handlers`. Un gestionnaire en ligne dans le gabarit ne fait qu'une ligne claire ; une fonction fléchée ne sert qu'à passer une variable du scope du gabarit en plus du payload.

### Typage

- [ ] Aucun `any` : ni `: any`, ni `as any`, ni `ref()` sans type quand l'inférence donne `any`, ni `Record<string, any>`.
- [ ] Les DTO et services viennent du client généré (`opensilex-core/index`, `opensilex-security/index`, `opensilex-core/api/<x>.service`). Les types d'UI viennent de naive-ui (`DropdownOption`, `DataTableColumns<T>`, `SelectOption`, `TreeSelectOption`, `FormRules`, `FormInst`, `DataTableRowKey`).
- [ ] Une interface locale existe quand aucun type ne colle à la forme manipulée.
- [ ] Une assertion de type ciblée n'est tolérée **qu'à la frontière** d'un composant partagé faiblement typé (payload `any` de `select`, `searchMethod: Function` de `TableAsyncView`). Elle ne se propage pas : le reste du composant manipule le type précis.
- [ ] `defineEmits` en syntaxe tableau nommé : `toto: [payload: string]`.
- [ ] Refs de gabarit : `useTemplateRef<InstanceType<typeof Composant>>('nom')`, ou le type d'instance naive-ui (`FormInst`…). Jamais `ref<any>(null)` ni `ref(null)`.

### UI

- [ ] Plus aucun composant Bootstrap (grille `row`/`col`, `btn`, `card`, `badge`, `table`, `form-group`, `dropdown`, `modal`, `nav-tabs`, `data-bs-*`). Chaque équivalent Naive UI est dans `bootstrap-to-naive.md`. Les utilitaires d'espacement (`mb-2`, `p-3`, `d-flex`) sont tolérés.
- [ ] Pas de classe Bootstrap résiduelle posée sur un composant Naive UI (`<n-button-group class="btn-group btn-group-sm">`).
- [ ] Dans l'hôte, chaque `n-*` utilisé est importé depuis `naive-ui` (ou figure dans `create({ components })` de `main.ts` sur la branche courante). Dans un **module d'extension**, qui n'importe pas naive-ui, il doit figurer dans cette liste globale. Sinon la balise reste inerte, sans aucune erreur.

### Réutilisation

- [ ] Rien n'est réinventé de ce qui existe dans `components/common/` : `TableAsyncView`, `SearchFiltersSidebar`, `FormSelector` et les sélecteurs métier, les `*Button`, `Card`, `Modal`, les champs `*Form` (catalogue dans `common-components.md`).
- [ ] Aucun composant partagé n'a été modifié sans demande explicite. Si un bug y est trouvé, il est **signalé**, pas corrigé en douce.
- [ ] Aucun sélecteur bâti sur `FormSelector` n'est enveloppé d'un `n-form-item` : il rend déjà son libellé (`FormField`) et son `n-form-item path`. Le doublon donne deux libellés et deux messages de validation.
- [ ] Les filtres sont passés comme slot à `SearchFiltersSidebar` ; le composant ne contient pas de second bouton Rechercher ou Réinitialiser.

### Code mort et fidélité de la migration

- [ ] Pas de prop, d'événement, de ref, d'import, de computed ou de méthode inutilisés.
- [ ] Pas de champ dans l'objet de filtres sans contrôle d'UI qui le modifie, sauf s'il vient de la route ou d'une prop (et c'est alors commenté).
- [ ] Pas de code commenté recopié de l'ancien composant.
- [ ] Les raccourcis d'un composant déjà migré n'ont pas été recopiés tels quels (voir « Ne pas recopier » dans `SKILL.md`).

### Comportement

- [ ] Les appels d'API passent par `$opensilex.getService<…>` et les erreurs par `$opensilex.errorHandler(error)`. Aucun `catch` ne reste vide.
- [ ] Les droits sont vérifiés avec `user.hasCredential(credentials.CREDENTIAL_…_ID)` pour toute action de modification ou de suppression.
- [ ] Aucun texte en dur dans le gabarit : chaque clé existe en **fr et en**, dans le bloc `<i18n>` (préfixé par le nom du composant) ou dans `lang/message-*.json` pour les clés partagées.
- [ ] Les watchers et `onMounted` ne déclenchent pas deux fois la même recherche au chargement.
- [ ] Aucun `console.log` de débogage.

## Format du rapport

Constats triés du plus grave au moins grave, un par ligne (exemple fictif) :

```
[BLOQUANT] components/foo/FooList.vue:120  template-ref  ref<any>(null) pour la table → useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef')
[À CORRIGER] components/foo/FooList.vue:42  bootstrap  <span class="badge"> → <n-tag size="small" round>
[SUGGESTION] components/foo/FooList.vue:88  dead-code  filters.brand n'a aucun contrôle dans la sidebar → supprimer ou ajouter le champ
```

- **BLOQUANT** : casse le comportement, viole le typage (`any`, ref de gabarit non typée), ou modifie un composant partagé sans demande.
- **À CORRIGER** : convention violée (structure, Bootstrap, emits, régions, i18n manquante, code mort).
- **SUGGESTION** : lisibilité, nommage, JSDoc manquante sur une méthode non évidente.

Terminer par ce qui a été vérifié et ce qui ne l'a pas été. Le build passe-t-il ? La vérification de types a-t-elle pu tourner ? Le rendu dans le navigateur reste à vérifier par l'utilisateur.
