# 👤 Modèle `User` – Gestion des utilisateurs dans OpenSilex

Le fichier `User.ts` contient la définition du modèle `User`, utilisé pour représenter l’état de l’utilisateur dans l’application.

---

## 📦 Responsabilités principales

- Modéliser les données de l’utilisateur courant (connecté, anonyme)
- Extraire les données depuis un token JWT retourné par l'API
- Fournit les méthodes pour gérer l’authentification via token ou cookie
- Permettre la vérification d'authentification
- Accès aux informations nécessaires au front (rôles, email, nom, etc).
---

## 🔐 Constructeur statique : `fromToken(token: string)`

```ts
static fromToken(token: string): User
```

Permet de créer un objet `User` à partir d’un JWT. Le token est fournit par l'API suite à une authentification ou récupéré dans l'URL, puis décodé localement pour récupérer :

- email

- roles / droits

- language

- name

- token expiration

Utilisé dans `main.ts` lors de l'initialisation, par exemple :

```ts
if (urlParams.has("token")) {
  const user = User.fromToken(urlParams.get("token"));
}
```

## 🍪 Cookie d'authentification
Le token utilisateur peut être sauvegardé dans un cookie via :

`$opensilex.setCookieValue(user);`

Et récupéré automatiquement via :

`user = $opensilex.loadUserFromCookie();`

## ✅ Vérification de connexion

`user.isLoggedIn()`
Renvoie `true` si le token est valide et que l'utilisateur n'est pas considéré comme "anonymous".


## 🔧 Méthodes principales

| Méthode            | Description                              |
| ------------------ | ---------------------------------------- |
| `fromToken(token)` | Crée un `User` depuis un token JWT       |
| `isLoggedIn()`     | Vérifie que le token est valide          |
| `getEmail()`       | Retourne l’email de l'utilisateur        |
| `getLocale()`      | Langue préférée (extraite du token)      |
| `getRoles()`       | Liste des rôles associés à l'utilisateur |
| `getName()`        | Nom complet ou identifiant utilisateur   |


## 🔁 Exemple d'utilisation dans l'app

```ts
let user = User.fromToken(jwt);
store.commit("login", user);

```

## 🔐 Intégration avec les systèmes d’authentification OpenID & SAML
En dehors de l'authentification standard par token (via API), Les méthodes `fromToken` et `setCookieValue` permettent également de gérer des connexions externes (OpenID, SAML), en récupérant le token transmis dans l’URL après redirection.

```ts
if (baseURL.endsWith("/openid") && urlParams.has("code")) {
  authService.authenticateOpenID(code).then(response => {
    const user = User.fromToken(response.token);
    $opensilex.setCookieValue(user);
  });
}
```

## 📝 Attributs du modèle User

| Propriété   | Type      | Description                           |
| ----------- | --------- | ------------------------------------- |
| `email`     | string    | Email principal                       |
| `name`      | string    | Nom ou identifiant                    |
| `roles`     | string[]  | Rôles OpenSilex (ex: `ADMIN`, `USER`) |
| `locale`    | string    | Code langue (`en`, `fr`, etc.)        |
| `token`     | string    | Token JWT original                    |
| `expiresAt` | Date      | Date d’expiration du token            |

❗ Remarques
- Le modèle User ne fait aucun appel réseau : il s’appuie uniquement sur les tokens (JWT).

- La validation du token côté API est gérée par les services backend.

## 📚 Voir aussi

- [`main.ts`](./main.md) – Utilisation du User dans l’app

- [`OpenSilexVuePlugin.ts`](./plugin.md) – Méthodes pour gérer les tokens / cookies