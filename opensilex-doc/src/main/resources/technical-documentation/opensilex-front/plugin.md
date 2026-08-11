# 🧩 OpenSilexVuePlugin – Plugin Vue pour OpenSilex

Le fichier `OpenSilexVuePlugin.ts` définit le **plugin central** de l’application Vue. Il facilite l’interaction entre la couche front-end et l’API. Il permet de fournir les services à l'ensemble des composants de l'application.

---

## 🎯 Rôles du plugin

- Gérer la communication avec l’API REST
- Fournir des services (authentification, configuration, modules, etc.)
- Charger dynamiquement les **modules Vue** et **composants associés**
- Gérer l’injection de thèmes, polices, routes, et traductions
- Agir en tant que passerelle entre les composants Vue et les données métier
- Intégrer la configuration globale dans l’application

---

## 🔧 Initialisation

```ts
const $opensilex = new OpenSilexVuePlugin(baseApi, store, null);
app.use($opensilex);
```

Le plugin est ensuite disponible globalement via :

`app.config.globalProperties.$opensilex`

Et accessible au travers de l'instance du store: 

`(store as any).$opensilex = $opensilex;`


## 📦 Méthodes principales

| Méthode                          | Description                                                |
| -------------------------------- | ---------------------------------------------------------- |
| `loadModules(modules: string[])` | Charge dynamiquement des modules OpenSilex                 |
| `getService<T>(name: string)`    | Retourne un service de l’API (ex: `AuthenticationService`) |
| `loadComponentModules()`         | Charge les composants associés aux modules                 |
| `loadComponentTranslations()`    | Charge les fichiers de traduction associés                 |
| `getConfig()`                    | Retourne la configuration globale depuis l’API             |
| `setConfig()`                    | Enregistre localement la configuration                     |
| `getThemeConfig()`               | Charge un thème CSS depuis un module                       |
| `setThemeConfig()`               | Applique la configuration du thème                         |
| `setCookieValue(user)`           | Sauvegarde le token utilisateur dans un cookie             |
| `loadUserFromCookie()`           | Charge un `User` à partir du cookie                        |
| `getResourceURI(path: string)`   | Construit une URL absolue pour une ressource               |
| `initAsyncComponents()`          | Prépare les composants asynchrones                         |


## 🔄 Cycle de vie typique
1. Initialisation du plugin dans `main.ts`

2. Chargement des modules via `loadModules(...)`

3. Récupération via `getConfig()` de la configuration, notamment les composants globaux de layout (thème)

4. Injection des services, thèmes, composants, traductions

5. Accès depuis les composants Vue via `$opensilex`


## 🌐 Exemple d'utilisation


```ts
const config = await $opensilex.getConfig();
const defaultFooterComponent = config.footerComponent
const authService = $opensilex.getService<AuthenticationService>("AuthenticationService");
```

## 🔌 Accès aux services REST & Migration Client

OpenSILEX évolue du modèle historique de localisation de service (`$opensilex.getService(...)`) vers des fonctions SDK typées issues d'OpenAPI 3.1.1 (`@hey-api/openapi-ts` et `@hey-api/client-fetch`).

### 🔴 Code Historique / Legacy (Inversify Service Locator)
Dans l'ancien code, les services étaient instanciés dynamiquement via une clé sous forme de chaîne de caractères :

```ts
// 🔴 Ancien modèle : Récupération dynamique de service via string key
import { UriSearchService } from "opensilex-core";

const uriSearchService = ref<UriSearchService>();
uriSearchService.value = $opensilex.getService("opensilex.UriSearchService");

uriSearchService.value.getUriTypes()
  .then((response: any) => {
    console.log("Types:", response.result);
  })
  .catch((error: any) => {
    $opensilex.errorHandler(error);
  });
```

### 🟢 Code Moderne (OpenAPI SDK Client)
Dans le nouveau code, les fonctions d'API sont directement importées depuis les modules front (`opensilex-security`, `opensilex-core`, etc.) et s'utilisent en `async`/`await` :

```ts
// 🟢 Nouveau modèle : Fonctions SDK directement importées
import { getUriTypes } from 'opensilex-core';
import { authenticate } from 'opensilex-security';

try {
  const { data, error } = await getUriTypes();
  if (error || !data) throw error;
  console.log("Types:", (data as any)?.result ?? data);
} catch (error: any) {
  $opensilex.errorHandler(error);
}
```

## 🎨 Thèmes et ressources
Les fichiers CSS de thème sont récupérés via l’API :

`GET @Path("/theme/{moduleId}/{themeId}/style.css")`

Et appliqués dynamiquement dans le DOM.

## 🗂️ Composants dynamiques
Lorsqu’un module est chargé, ses composants sont aussi enregistrés :

```ts
$opensilex.loadComponentModules([
  ModuleComponentDefinition.fromString("module:Component")
])
```
(voir également `index.ts`).

Les traductions associées sont aussi injectées dans `vue-i18n`.

## 🔐 Gestion des utilisateurs
Le plugin inclut une gestion intégrée de l'utilisateur :

`$opensilex.setCookieValue(user);`
Stocke le token JWT dans un cookie.

`const user = $opensilex.loadUserFromCookie();`
Tente de restaurer l'utilisateur depuis le cookie enregistré.


## 🔄 Intégration avec Vue
Dans `main.ts`: 

```ts
const $opensilex = new OpenSilexVuePlugin(baseApi, store, null);
app.use($opensilex);
app.provide("$opensilex", $opensilex);
```
Cela rend le plugin accessible globalement via `$opensilex` ou `inject('$opensilex')`.

### 🔍 Relation avec main.ts
Le fichier `main.ts` utilise le plugin pour :

- Charger les modules backend
- Récupérer les composants à afficher
- Initialiser la configuration
- Gérer le thème, les polices, et les traductions
- Authentifier l’utilisateur
- Intégrer dynamiquement les routes


## 📚 Voir aussi

- [`main.ts`](./main.md) – Initialisation et usage du plugin

- [`User.ts`](./user.md) – Gestion des tokens utilisateur

- [`OpenSilexRouter.ts`](./router.md) – Intégration du routeur
