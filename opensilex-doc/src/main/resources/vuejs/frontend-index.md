# 🌿 OpenSilex Frontend Vue 3

Cette documentation aborde l'interface front-end de la plateforme **OpenSilex**, développée avec **Vue.js 3**. Elle offre une interface modulaire, personnalisable, multi-utilisateurs et connectée à des APIs REST.

---

## 🧱 Architecture générale

```
opensilex-front/
│
├── src/
│   ├── main.ts                # Point d’entrée de l’application
│   ├── models/                # Modèles métiers (User, Store, etc.)
│   ├── components/            # Composants Vue globaux
│   ├── lib/                   # Services, plugins, types partagés
│   ├── lang/                  # Dictionnaires de traduction
│   └── App.vue                # Composant racine
│
├── public/                   # Fichiers statiques (index.html, favicon, etc.)
└── README.md                 # Documentation principale
```

---


## 🧩 Technologies utilisées

- **Vue 3**
- **Vuex** (store global)
- **Vue Router**
- **Naive UI** (composants UI)
- **Vue I18n** (internationalisation)
- **FontAwesome**, **Bootstrap5**
- **Highcharts**
- **OpenSilexVuePlugin** (plugin d'intégration)

---

## 📂 Documentation technique

- [`main.ts`](./main.md) – Point d’entrée principal
- [`User.ts`](./user.md) – Modèle utilisateur et auth
- [`OpenSilexVuePlugin.ts`](./plugin.md) – Plugin d'intégration OpenSilex
- [`OpenSilexRouter.ts`](./router.md) – Routage dynamique
- [`Store.ts`](./store.md) – Gestion des droits et de la session

---

## 📌 Environnements

| Variable | Description |
|----------|-------------|
| `DEV_BASE_API_PATH` | URL d’API en développement (`main.ts`) |
| `isDevMode`, `isDebug` | Active les logs et comportements spécifiques |

---