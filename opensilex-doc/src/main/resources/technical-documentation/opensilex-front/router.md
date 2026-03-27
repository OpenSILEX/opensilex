# 🚦 OpenSilexRouter – Routage modulaire Vue 3

Le fichier `OpenSilexRouter.ts` fournit un système de **routage dynamique** utilisé dans l'application Vue.js OpenSilex. Il permet d’ajouter dynamiquement des routes à partir des modules chargés et de centraliser la logique de navigation dans un store Vuex.

---

## 🎯 Objectifs

- Définir un routeur Vue accessible dans toute l'application
- Ajouter des routes de manière dynamique en fonction de la configuration
- Supporter différents layouts (composant par défaut, etc.)
- Faciliter l’encapsulation de la logique de navigation

---

## 🧱 Fonctionnement général

L’objet principal est une **classe singleton** `OpenSilexRouter` qui :

1. Initialise un routeur Vue 3 (`createRouter`)
2. Expose des méthodes pour :
   - Ajouter dynamiquement des routes
   - Accéder au routeur
   - Réinitialiser toutes les routes

---

## 🔄 Cycle typique

Dans `main.ts` :

```ts
store.commit("resetRouter");
let router = store.state.openSilexRouter.getRouter();
app.use(router);
```

Puis les routes sont ajoutées dynamiquement à partir de la configuration :

```ts
router.addRoute({
  path: "/some-path",
  component: MonComposant
});
```
---

## 🧩 Méthodes clés

| Méthode                      | Description                             |
| ---------------------------- | --------------------------------------- |
| `getRouter()`                | Retourne l’instance `vue-router`        |
| `addRoute(route)`            | Ajoute une nouvelle route dynamiquement |
| `reset()` ou `resetRouter()` | Réinitialise les routes existantes      |
| `init()`                     | Crée et configure le routeur Vue        |

`reset(): void`
Réinitialise le routeur en supprimant toutes les routes et en réinstanciant l’objet Router. Nécessaire lorsque :
- On change d’utilisateur

- L’application est rechargée dans un contexte d’iframe (mode embed)

- Le layout ou les modules visibles changent dynamiquement


## 🗺️ Génération dynamique des routes

Les routes sont définies dans le fichier YAML de configuration comme suit :
```ts
routes:
  - path: /variables_group/details/:uri
    component: opensilex-GroupVariablesDescription
  - path: /variables_group/annotations/:uri
    component: opensilex-GroupVariablesDescription
```

Et pour le menu utilisateur : 
```
menu:
  - id: scientific-organisation
    label: component.menu.scientific-organisation
    children:
      - id: organizations
        label: component.menu.organization
        route:
          path: /organizations
          component: opensilex-OrganizationView
```

### Cycle de transformation (backend → frontend)
1. Le backend injecte la configuration dans les objets :
- `FrontConfigDTO` (pour routes)
- `UserFrontConfigDTO` (pour menu)

2. Ces objets sont récupérés dans le frontend (dans `main.ts` via `$opensilex.getConfig()` et `$opensilex.getUserConfig()`)


3. Puis les routes sont générées dynamiquement dans `OpenSilexRouter.computeMenuRoutes()` :
```ts
routes.push({
  path: routeConfig.path,
  name: routeConfig.name || undefined,
  component: defineAsyncComponent(() => this.getAsyncComponentLoader(routeConfig.component)),
});
```

## 🧪 Intégration avec le mode embed
L’URL peut contenir ?embed=true, ce qui active un mode d'affichage simplifié sans :
- Header
- Footer
- Menu

Dans ce mode, seules les routes essentielles sont montées.


## 🔧 Exemple d’ajout manuel

```ts
store.commit("resetRouter");
const router = store.state.openSilexRouter.getRouter();

app.use(router);

router.addRoute({
  path: "/dashboard",
  name: "dashboard",
  component: Dashboard
});

```

## 🧼 Réinitialiser les routes
Pour effacer les routes existantes et reconfigurer le routeur :

`store.commit("resetRouter");`

❗ Remarque importante
Toutes les routes doivent être ajoutées avant l’appel à app.mount('#app').


## 🧠 Interaction avec le système de composants dynamiques (index.ts)
Le routeur ajoute des routes en fonction des noms de composants définis par la configuration backend (ex : `"core:Header"`, `"security:Login"`).

Ces noms sont transformés en `ModuleComponentDefinition`, puis associés à de véritables composants Vue grâce à un système de chargement dynamique défini dans `components/index.ts` :

`const modules = import.meta.glob('./**/*.vue', { eager: true });`

Ce fichier :

1. Importe tous les fichiers `.vue` du dossier `/components` (et sous-dossiers)

2. Génère une map `components` avec des clés comme `"opensilex-Header"`

3. Associe dynamiquement ces composants à leurs noms

Ensuite, dans `main.ts` :

```ts
for (let componentName in components) {
  app.component(componentName, components[componentName]);
}
```

Et dans `OpenSilexVuePlugin`, lors du chargement :

ModuleComponentDefinition.fromString("core:Header") 
→ "Header"
→ "opensilex-Header"
→ correspondance dans components


## 📚 Voir aussi

- [`main.ts`](./main.md) – Utilisation du routeur dans le cycle d'initialisation

- [`OpenSilexVuePlugin.ts`](./plugin.md) – Chargement dynamique des composants et routes via plugin
