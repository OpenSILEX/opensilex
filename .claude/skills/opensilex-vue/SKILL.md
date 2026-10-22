---
name: opensilex-vue
description: Créer, modifier, migrer de Vue 2 vers Vue 3, ou relire un composant .vue du front OpenSILEX selon les conventions de l'équipe. Couvre l'ordre template/script setup/style/i18n, les régions //#region Public/Private, defineEmits en syntaxe tableau, useTemplateRef typé, Naive UI à la place de Bootstrap, la réutilisation des briques common/ (TableAsyncView, SearchFiltersSidebar, FormSelector, boutons d'action), zéro any et la suppression du code mort, selon le document vuejs-file-structure.md de la doc technique. À utiliser dès qu'on écrit, porte, corrige ou revoit un composant Vue OpenSILEX (liste, filtres, formulaire, page de détail, sélecteur), même si l'utilisateur ne parle pas de « conventions ». Create, modify, migrate or review OpenSILEX Vue 3 components.
---

# Composants Vue OpenSILEX

Ce skill fixe **comment s'écrit un composant**. La structure du fichier (régions, ordre, gestionnaires, typage, JSDoc) vient de la documentation technique, [`vuejs-file-structure.md`](../../../opensilex-doc/src/main/resources/technical-documentation/opensilex-front/vuejs-file-structure.md) : c'est elle qui fait foi, et ce skill la complète par les règles d'UI, d'i18n, de réutilisation et de migration. Si le document change, aligne ce skill dessus.

La **plateforme** (`$opensilex` et les services d'API, routes et menus `opensilex.front.yml`, modules d'extension, build) est décrite dans `opensilex-doc/src/main/resources/technical-documentation/opensilex-front/` (`plugin.md`, `router.md`, `vite.md`) et `opensilex-module/module-frontend-loading.md`. Le skill `opensilex-vue3`, s'il est installé sur ton poste, la couvre aussi ; sur la forme d'un composant, ce skill-ci l'emporte.

Le front hôte est dans `opensilex-front/front/src`, les briques partagées dans `components/common/`.

## Choisir le mode

| Demande | Déroulé |
|---|---|
| **Créer** un composant | 1. Chercher la brique existante (`references/common-components.md`, `graft grep`). 2. Partir de `references/component-skeleton.md`, et de `references/list-page.md` pour une liste. 3. Écrire, puis passer « Vérifier ». |
| **Migrer** un composant Vue 2 | 1. Lire l'original (`git show origin/develop:<chemin>`, la branche `develop` est encore en Vue 2) et inventorier ce qui sert vraiment : filtres, colonnes, actions, événements, props. 2. Écarter le code mort au lieu de le porter. 3. Réécrire selon les conventions avec les briques Vue 3. 4. « Vérifier », puis lister ce qui a été volontairement abandonné. |
| **Modifier** un composant existant | Le code que tu écris suit les conventions. Ne reformate pas tout le fichier sans demande : signale les écarts restants (sortie du script) dans ton résumé. Exception : en cours de migration, le fichier entier doit être conforme. |
| **Relire** (revue) | Suivre `references/review-checklist.md` : script, grille manuelle, puis rapport trié par gravité. |

## Conventions

### Fichier

- Blocs dans l'ordre **`<template>` → `<script setup lang="ts">` → `<style scoped lang="scss">` → `<i18n>`**. Pas de bloc vide.
- Dans `<script setup>`, après les imports et les types locaux, ces régions, avec **ces noms exacts** (le dépôt en compte une trentaine de variantes, ne pas les multiplier) :
  ```ts
  //#region Public
  //   defineEmits → defineProps (withDefaults) → defineModel
  //#endregion

  //#region Private
  //#region Plugins & services     useI18n, useRoute, useStore, inject('$opensilex'), getService
  //#region Template refs
  //#region Data & Computed
  //#region Hooks & watchers
  //#region Event handlers         on<Enfant><Événement>(payload: Type)
  //#region Methods
  //#endregion

  defineExpose({ … })
  ```
  Chaque sous-région se ferme par son `//#endregion`. Un petit composant peut se limiter à `Public` / `Private`, à condition que les éléments de même nature restent groupés.
- **Gestionnaires d'événements** : l'événement `foo` d'un enfant `Child` est géré par `onFoo` ou `onChildFoo(payload: Type)`, dans la région `Event handlers`, payload typé. Un gestionnaire d'une ligne peut rester en ligne dans le gabarit s'il reste clair (`@updated="emit('saved', item)"`). S'il faut une variable du scope du gabarit en plus du payload, une fonction fléchée appelle la méthode : `@changed="(payload) => onNestedChanged(payload, data)"`.
- **`defineEmits`** en syntaxe tableau nommé : `defineEmits<{ toto: [payload: string]; closed: [] }>()`. Pas la syntaxe d'overload `(e: 'toto', payload: string): void`, ni `defineEmits(['toto'])`. N'émettre que des événements déclarés, et ne déclarer que ceux qu'on émet.
- **Refs de gabarit** : `const table = useTemplateRef<InstanceType<typeof TableAsyncView>>('table')`. Pour un composant naive-ui qui expose un type d'instance : `useTemplateRef<FormInst>('formRef')`. Jamais `ref<any>(null)`, `ref(null)` ni `useTemplateRef<any>`.
- **Injection** : `const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!`, avec ce nom, cette clé et le `!`. Services : `$opensilex.getService<XService>('opensilex.XService')`, **une fois**, au niveau du `setup`.
- Une méthode ou une variable dont le rôle n'est pas évident porte un commentaire JSDoc `/** … */`.
- Texte affiché : uniquement par l'i18n, en **fr et en**. Le bloc `<i18n>` est en YAML et préfixé par le nom du composant. Pas de clé empruntée au bloc `<i18n>` local d'un **autre** composant : elle n'existe pas ici.
- **Clé ou texte traduit ?** Les composants OpenSILEX qui traduisent eux-mêmes reçoivent une **clé** : `label` des boutons, `Card`, `StringView`, `TableField.label` de `TableAsyncView`, `createTitle` / `editTitle` de `useModalFormLogic`, `searchButtonLabelTranslationKey`. Les props `n-*`, `TableView` et le `placeholder` de `FormSelector` reçoivent `t(…)`. Avant de passer `t(...)` à un composant enfant, ouvre-le.

### Typage : zéro `any`

Le `tsconfig` est permissif (`noImplicitAny: false`, `strictNullChecks: false`) et le build ne vérifie pas les types : rien ne t'arrêtera, la discipline est manuelle.

- **DTO et services** : ceux du client généré, en `import type` (`opensilex-core/index`, `opensilex-security/index`, `opensilex-core/api/<x>.service`, `opensilex-core/HttpResponse`).
- **Types d'UI** : ceux de naive-ui (`DropdownOption`, `DataTableColumns<T>`, `SelectOption`, `TreeSelectOption`, `FormRules`, `FormInst`, `DataTableRowKey`).
- **Store** : `useStore() as OpenSilexStore` (`@/models/Store`).
- **Interface locale** quand aucun type existant ne correspond à la forme manipulée : filtres d'une liste, options de `searchMethod`, ligne agrégée.
- **Assertion ciblée** tolérée **à la frontière** d'un composant partagé faiblement typé (`searchMethod: Function` de `TableAsyncView`, `data.item` d'un slot `#cell`, payload `any` d'un `select`). Elle ne se propage pas : le paramètre du gestionnaire porte le type précis, et le reste du composant ne voit que lui.

### UI : Naive UI, pas Bootstrap

- Remplacer **systématiquement** les composants Bootstrap (grille `row`/`col`, `btn`, `card`, `badge`, `table`, `form-group`, `dropdown`, `modal`, `nav-tabs`, `data-bs-*`) par leur équivalent Naive UI : `n-layout`, `n-grid`, `n-form-item`, `n-data-table`, `n-dropdown`, `n-tag`, `n-tabs`… Correspondances dans `references/bootstrap-to-naive.md`.
- Pas de classe Bootstrap laissée sur un composant naive (`<n-button-group class="btn-group btn-group-sm">`). Les utilitaires d'espacement (`mb-2`, `p-3`, `d-flex`) sont tolérés.
- **Importer chaque `n-*`** depuis `naive-ui` dans le composant (`import { NCard, NTag } from 'naive-ui'`). Seuls quelques composants sont globaux (`create({ components: […] })` dans `opensilex-front/front/src/main.ts`). La liste dépend de la branche (sur `vue3/main` : `NButton`, `NDataTable`, `NDropdown`, `NTree`) : lis-la sur la branche courante avant d'affirmer qu'un `n-*` est global. Un `n-*` non importé et non global ne rend rien, sans erreur.

### Réutiliser, sans modifier

- Chercher dans `components/common/` avant d'écrire quoi que ce soit : table asynchrone (`TableAsyncView`), panneau de filtres (`SearchFiltersSidebar`), sélecteurs (`FormSelector` et la trentaine de sélecteurs métier bâtis dessus), boutons d'action (`CreateButton`, `EditButton`, `DeleteButton`, `DetailButton`), `Card`, `Modal` + `useModalFormLogic`, champs `*Form`. Catalogue et API : `references/common-components.md`.
- **Ne pas modifier ces briques** sans demande explicite : d'autres listes et vues migrées en dépendent. Un défaut s'y signale ; il ne se corrige pas en douce.
- **Avant d'envelopper un composant enfant, ouvre son gabarit.** Un sélecteur bâti sur `FormSelector` rend déjà son libellé et son `n-form-item path` : il ne s'enveloppe pas. Les champs `*Form` (`InputForm`…) se placent dans un `n-form-item path` **sans** `label`, c'est le champ qui porte le libellé. Tableau complet dans `references/common-components.md`.
- N'utiliser que ce qu'une brique **expose** (`defineExpose`) et **émet** réellement. Lire un état interne non exposé renvoie `undefined`, sans erreur.

### Code mort

Une migration n'est pas une copie fidèle bug pour bug. Supprimer ce que l'ancien composant traînait :

- props, refs, imports, computed ou méthodes inutilisés ;
- champs de filtre sans contrôle d'UI correspondant ;
- props passées à un enfant qui ne les déclare pas, souvent héritées de l'ancien `opensilex-ModalForm` (`initForm`, `successMessage`, `modalSize`) ;
- code commenté et `console.log` ;
- syntaxes Vue 2 mortes (`.sync`, `this.$refs`, `$listeners`).

Dire dans le résumé ce qui a été retiré, pour que l'utilisateur puisse contester.

### Ne pas recopier un composant déjà migré sans le questionner

Le code existant est hétérogène : sur 321 `.vue`, 118 ont encore les emits en overload, 53 `ref<any>(null)`, 162 contiennent `any`, et seuls 26 ont la région `Public`. Un composant migré est une source d'**API** (quelles props, quels événements), pas un **modèle de style**. Raccourcis repérés dans les listes migrées, à ne pas reproduire :

- `:filtersCollapsed="…"` au lieu de `v-model:filtersCollapsed` sur `SearchFiltersSidebar` (9 listes), ce qui empêche le panneau de se refermer après une recherche ;
- `n-form-item` autour de `TypeForm` ou d'un sélecteur bâti sur `FormSelector`, plus le hack CSS `:deep(.compact-form-item)` pour cacher le libellé vide ;
- lecture d'états internes de `TableAsyncView` non exposés (`onlySelected`, `selectAll`, `totalRow`) et écoute de `@refreshed` / `@selectall`, jamais émis ;
- `t(...)` passé à un composant qui traduit lui-même (double traduction) ;
- `class="btn-group btn-group-sm"` sur `n-button-group`, `badge badge-pill`, `row` / `col` ;
- `ref<any>(null)`, `useTemplateRef<any>`, `as any`, `null as any` ;
- `n-*` utilisé sans import (`<n-p>` dans `ProjectList`) ;
- filtres réinitialisés en recopiant le littéral au lieu d'une fabrique `getEmptyFilter()`.

Vérifie ce qui est correct au lieu de reproduire le voisin.

## Vérifier

1. **Conventions**, sur les fichiers touchés :
   ```bash
   .claude/skills/opensilex-vue/scripts/check-vue-conventions.sh <fichiers.vue>
   ```
   Sans argument, il prend les `.vue` modifiés par rapport à `HEAD`. Il ne doit rester aucune `ERROR` dans le code que tu as écrit.
2. **Build**, avec Node 22 (le Node 18 système fait planter rolldown) :
   ```bash
   export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"; cd opensilex-front/front && nvm exec 22 npm run build
   ```
   `vue`, `vite` et `naive-ui` sont déclarés dans le `package.json` **racine**. Si le build échoue sur `Cannot find module 'vite'` ou `'naive-ui'`, ces dépendances ne sont pas installées : le dire, et proposer `nvm exec 22 npm install` à la racine plutôt que de le lancer sans accord.
3. **Types : le dire franchement.** `npm run build` (`vite build`) **ne vérifie pas les types**. Il efface les `import type`, donc un mauvais chemin de type ou un `any` passent. `vue-tsc` n'est pas une dépendance du projet. De plus, `opensilex-core/front/types/opensilex-core.d.ts`, que le `tsconfig` référence, est généré par le build Maven et peut manquer. Ne prétends donc jamais à une vérification de types complète. Écris plutôt : « build OK, vérification de types (vue-tsc) non disponible dans le projet ».
4. **Navigateur** : le fonctionnement réel (rendu, filtres, actions, sélection) est vérifié **par l'utilisateur**, sauf s'il demande explicitement que tu le fasses. Termine en listant ce qu'il doit regarder.

## Références

- `references/component-skeleton.md` : squelette complet et conforme sur un cas concret, plus la version courte.
- `references/list-page.md` : page de liste (`n-layout` + `SearchFiltersSidebar` + `TableAsyncView` + actions), vue propriétaire du formulaire, détail et onglets.
- `references/common-components.md` : API des briques `common/`, qui enveloppe quoi, ce qui est global, défauts connus.
- `references/bootstrap-to-naive.md` : correspondances Bootstrap → Naive UI, avec exemple.
- `references/review-checklist.md` : grille de revue et format du rapport.
- `scripts/check-vue-conventions.sh` : contrôle mécanique des conventions.
- Source des conventions : `opensilex-doc/src/main/resources/technical-documentation/opensilex-front/vuejs-file-structure.md`.
