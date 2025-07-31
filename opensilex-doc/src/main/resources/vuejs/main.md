# 📌 main.ts – Point d’entrée de l’application OpenSilex

Ce fichier initialise toute l’application Vue.js : API, plugins, composants, thème, configuration, utilisateur et rendu principal.

---

## 🧠 Objectif principal

- Initialiser Vue 3 avec ses plugins
- Définir le point de montage de l’application
- Charger dynamiquement les modules, composants, configuration et thème
- Authentifier l’utilisateur (guest ou non)
- Gérer le routage via un store centralisé

---

## 🏗️ Étapes de l'initialisation

### 1. 🔗 Détection de l’environnement

```ts
const DEV_BASE_API_PATH = "http://localhost:8666/rest";
let isDebug = true;
let isDevMode = true;
```
Ces variables contrôlent le comportement de l'API et des logs


### 2. 🈯  Configuration i18n (internationalisation)

```ts
const i18n = createI18n({
  fallbackLocale: 'en',
  locale: lang,
  messages: { en, fr },
  dateTimeFormats: { ... },
  numberFormats: { ... }
});
```
Chargement multilingue ```(en, fr)``` avec gestion des formats date/numérique.


### 3. 🧩 Chargement des plugins Vue

| Plugin / Lib         | Rôle principal          |
| -------------------- | ----------------------- |
| `naive-ui`           | Composants Vue modernes |
| `bootstrap`,          | Styles de base          |
| `fontawesome`, `bootstrap-icons`        | Icônes vectorielles     |
| `HighchartsVue`      | Graphiques interactifs  |
| `OpenSilexVuePlugin` | Plugin OpenSilex custom |



### 4. 🧠 Création de l’app Vue

```ts
const app = createApp(App);
app.use(i18n)
app.use(store);
app.use(naive);
app.use(HighchartsVue);
app.component('font-awesome-icon', FontAwesomeIcon);
```
Toutes les librairies doivent impérativement être liées à `app` avant son montage (`app.mount('#app')`)

### 5. 📦 Chargement des composants globaux

```ts
for (let componentName in components) {
  app.component(componentName, components[componentName]);
  $opensilex.loadComponentTranslations(component);
}
```
Tous les composants de `./components` sont enregistrés dynamiquement, il n'y a donc plus besoin de les déclarer, un par un, manuellement dans un fichier dédié.


### 6. 🎨 Thème & polices
- Les polices sont chargées à partir de la config API

- Le thème CSS est injecté dynamiquement si activé

```ts
vueJsService.getThemeConfig(...)
```


### 7. 👤 Authentification de l’utilisateur
Ordre de priorité :

1. Token dans l’URL ```(?token=...)```

2. Cookie

3. Anonyme ```(user.isLoggedIn() === false)```

Supporte :

- OpenID Connect

- SAML

- Auth locale


### 8. 🧩 Chargement dynamique des modules
```ts
$opensilex.loadModules([
  "opensilex-security",
  "opensilex-core",
]);
```

Puis on appelle :

```
$opensilex.loadComponentModules(modulesToLoad)
```


### 9. 🚦 Routage
Initialisé à partir du store :

```ts
store.commit("resetRouter");
let router = store.state.openSilexRouter.getRouter();
app.use(router);
```

Routes principales et composants de layout présents globalement (issues de la config) :

- `homeComponent`

- `notFoundComponent`

- `headerComponent`

- `footerComponent`

- `loginComponent`

- `menuComponent`



## ❓ Questions fréquentes
### ➕ Comment ajouter une nouvelle bibliothèque Vue ?
1. Installe-la via NPM :

`npm install ma-librairie`

2. Importe-la :
```ts
import MaLibrairie from 'ma-librairie';
app.use(MaLibrairie);
```
⚠️ Rappel : La librairie doit être liée à l'application avant le montage de app (`app.mount('#app')`)

### 🌍 Comment ajouter une langue supplémentaire ?
1. Crée un fichier ```message-xx.json``` dans ```/lang```

2. Ajoute-le à ```messages``` :

```ts
import de from './lang/message-de.json';
...
messages: { en, fr, jp }
```

### 🎨 Comment changer le thème par défaut ?
Défini ```themeModule``` et ```themeName``` dans la configuration retournée par l'API (ou dans un mock pour dev local).

### ⚙️ Comment changer l'API backend ?
Modifie `DEV_BASE_API_PATH` :

`const DEV_BASE_API_PATH = "http://localhost:PORT/rest";`

### 🚫 Comment désactiver les logs en production ?
```
if (!isDebug) {
  console.debug = function () {};
}
```

### 🔍 À explorer ensuite
- [`User.ts`](./user.md) – Authentification et modèle utilisateur

- [`OpenSilexVuePlugin.ts`](./plugin.md) – Plugin principal d'interaction API, gestion tokens / cookies

- [`OpenSilexRouter.ts`](./router.md) – Routage dynamique et modulaire



