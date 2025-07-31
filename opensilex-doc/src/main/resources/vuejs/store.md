# 🧠 Store.ts – Store global Vuex pour OpenSilex

Ce fichier centralise la **gestion de l’état global** de l'application OpenSilex via Vuex. Il permet :

- De gérer l’authentification, la session et son expiration
- De contrôler les menus, le routeur, le loader
- D’enregistrer la configuration front et utilisateur
- De stocker les **droits (credentials)** disponibles
- D'intégrer dynamiquement le routeur et l'utilisateur dans l'application

---

## 📦 Structure générale

### 📁 Modules importés

- `User.ts` : gestion du modèle utilisateur
- `OpenSilexVuePlugin.ts` : interface principale avec les services OpenSilex
- `OpenSilexRouter.ts` : instanciation et manipulation du routeur Vue
- `AuthenticationService` : pour les appels d'authentification à l’API
- `FrontConfigDTO`, `UserFrontConfigDTO` : configuration globale et utilisateur.

---

## 🧱 Contenu du `state`

| Clé                  | Description                                                                 |
|----------------------|-----------------------------------------------------------------------------|
| `user`               | Utilisateur courant (objet `User`)                                          |
| `openSilexRouter`    | Instance du routeur personnalisé                                             |
| `config`             | Configuration front retournée par l’API                                     |
| `userConfig`         | Configuration utilisateur personnalisée                                     |
| `menu`               | Menu actuel généré depuis les modules chargés                               |
| `menuVisible`        | Affichage ou non du menu                                                    |
| `loaderVisible`      | Affichage du loader                                                         |
| `disconnected`       | Indique si l'utilisateur est déconnecté                                     |
| `lang`               | Langue active                                                               |
| `previousPage[]`     | Historique de navigation                                                    |
| `search`             | Données de recherche (ex: expériences)                                      |
| `credentials`        | Droits/actions disponibles définis statiquement                             |

---

## 🔑 Gestion des droits (credentials)

Les `credentials` sont des **identifiants de droits d’accès** permettant d'autoriser ou non l'accès ainsi que certaines actions (CRUD) sur les entités métiers (projets, groupes, variables, etc.).

### Exemple :

```ts
CREDENTIAL_EXPERIMENT_MODIFICATION_ID: "experiment-modification"
CREDENTIAL_VARIABLE_DELETE_ID: "variable-delete"
```

Ces droits peuvent être utilisés dans les composants pour activer/désactiver des actions, rendre visible certaines section ou non, selon le rôle ou les permissions de l'utilisateur.


## ❓ Comment ajouter de nouveaux droits ?

1. Ajoute une nouvelle entrée dans `state.credentials` :

`CREDENTIAL_NEW_FEATURE_ACCESS_ID: "new-feature-access"`

2. Utilise cette clé dans les composants Vue avec un contrôle d'autorisation :
```ts
if (user.hasCredential(store.state.credentials.CREDENTIAL_NEW_FEATURE_ACCESS_ID)) {
  // Afficher ou activer la fonctionnalité
}
```
⚠️ Les valeurs doivent correspondre aux rôles / droits définis côté API


## 🔐 Gestion de session
Un système de timeout (`expireTimeout`) est mis en place pour déconnecter automatiquement l'utilisateur à expiration du token.

Un deuxième timer (`autoRenewTimeout`) permet de renouveler automatiquement le token à l'approche de l'expiration si une interaction utilisateur est détectée (clic, clavier, souris).


## ⚙️ Fonctions clés :
- `renewTokenOnEvent` : renouvelle le token sur activité utilisateur

- `login()` : installe les timers, charge le menu, l'utilisateur, le router

- `logout()` : nettoie l’état utilisateur et du router, supprime les cookies


## 🔁 Mutations principales

| Mutation            | Description                                    |
| ------------------- | ---------------------------------------------- |
| `login(user)`       | Authentifie, configure timers, router, menu    |
| `logout()`          | Déconnecte, supprime timers et état            |
| `setConfig(config)` | Initialise `config` et le routeur personnalisé |
| `setUserConfig()`   | Applique la configuration utilisateur          |
| `toggleMenu()`      | Affiche/masque le menu                         |
| `refresh()`         | Recharge le router                             |
| `lang(lang)`        | Définit la langue de l’utilisateur             |


## 🔄 Interaction avec le routeur

```ts
state.openSilexRouter = new OpenSilexRouter(config.pathPrefix, app);
state.openSilexRouter.setConfig(config);
state.openSilexRouter.setUserConfig(userConfig);
```

L'intégration du routeur est entièrement pilotée depuis le store : toutes les modifications d’état utilisateur ou config peuvent entraîner un reset du router.


## ⏳ Gestion du loader
`loaderVisible` est utilisé pour afficher ou masquer un composant de chargement.

Un `loaderCount` est incrémenté/décrémenté pour éviter les conflits d’affichage lors de requêtes multiples.


## 🔁 Navigation & historique
`storeCandidatePage(router)` : prépare la redirection en cas d’interruption

`validateCandidatePage()` : valide la page précédente stockée

`goBack()` : revient à la page précédente via l’historique local

## ❓ Questions fréquentes

### 🏠 Comment changer le composant d’accueil de l’application ?
Modifie dans `defaultConfig` ou via l’API le champ `homeComponent` :
`homeComponent: "opensilex-front-CustomHome"`

### 📤 Comment forcer la déconnexion manuellement ?
`store.commit("logout");`

### 🗯️ Comment afficher un message d’erreur ou une notification globale ?
Utilise l'instance du plugin Opensilex: 
`getOpenSilexPlugin()?.showErrorToast("Message d'erreur");`


## 📚 Voir aussi

- [`User.ts`](./user.md) – Authentification et modèle utilisateur

- [`OpenSilexVuePlugin.ts`](./plugin.md) – Plugin principal d'interaction API, gestion tokens / cookies

- [`OpenSilexRouter.ts`](./router.md) – Gestion du routage dynamique et modulaire