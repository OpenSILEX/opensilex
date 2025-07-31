# ⚙️ Configuration Vite pour OpenSilex

Le projet OpenSilex utilise des configurations Vite distinctes pour gérer :

- **L'application principale** (`opensilex-front/front`)
- **Les module réutilisable** ( ex : `opensilex-core/front`)

Ces configurations sont optimisées pour le développement, la compilation, l'internationalisation, le packaging en bibliothèque, et l’inclusion dans d’autres projets.

---

## 📁 Structure

| Fichier                             | Rôle principal                                  |
|-------------------------------------|--------------------------------------------------|
| `opensilex-front/front/vite.config.ts` | Application Vue complète, avec composants et routing |
| `opensilex-core/front/vite.config.ts`  | Module Vue exporté sous forme de bibliothèque réutilisable |

---

## ⚙️ `opensilex-front/front/vite.config.ts`


### 🔌 Plugins utilisés

- `@vitejs/plugin-vue` : support SFC Vue 3
- `vite-plugin-vue-devtools` : outils de debug Vue pendant le dev
- `@intlify/unplugin-vue-i18n` : support YAML/JSON pour i18n dans les `.vue`

```ts
plugins: [
  vue(),
  vueDevTools(),
  VueI18nPlugin({ defaultSFCLang: "yaml" })
],
```


### 🔗 Alias & résolution
```ts
resolve: {
  alias: {
    '~bootstrap': resolve(__dirname, 'node_modules/bootstrap')
  }
}
```
Permet d'importer Bootstrap plus facilement dans les composants.


### 📦 Build

```ts
build: {
  outDir: 'dist',
  rollupOptions: {
    external: ['vue'],
    output: {
      globals: {
        vue: 'Vue'
      }
    }
  }
}
```

- Exclut `vue` du bundle final
- Spécifie le dossier de sortie
- Configure les globales si le build est intégré dans une app UMD


### 🌐 Serveur de dev

```ts
server: {
  port: 8080
},
base: '/app/'
```
- Le serveur démarre sur le port `8080`
- Le chemin de base (`/app/`) est utilisé dans les URL des ressources statiques


## 🧩 `opensilex-core/front/vite.config.ts`

### 🧱 Objectif
Compiler une librairie Vue réutilisable dans d'autres modules.


### 🔌 Plugins utilisés
`plugins: [vue()]`
Pas d’i18n ni de devtools ici : configuration plus légère pour la compilation.


### 📦 Build ciblé bibliothèque

```ts
lib: {
  entry: resolve(__dirname, 'src/lib/index.ts'),
  name: 'opensilex-core',
  fileName: (format) => `opensilex-core.${format}.min.js`,
  formats: ['es', 'umd']
}
```
📦 Génère une bibliothèque compatible avec ES Modules et UMD.


### 🔗 Alias
```ts
resolve: {
  alias: {
    vue: resolve(__dirname, 'node_modules/vue')
  }
}
```
S'assure que `vue` point vers l'installation locale du module.


### 🧪 Optimisation
```ts
optimizeDeps: {
  include: ['vue']
}
```
Pré-bundle Vue pour éviter les recharges lentes et améliorer les performances de dev.


## ❓ FAQ Développeur

### ➕ Comment ajouter un nouveau plugin Vite ?

1. Installe-le :
`npm install vite-plugin-monplugin`

2. Déclare-le dans le tableau `plugins`:
```ts
import monPlugin from 'vite-plugin-monplugin';

plugins: [
  vue(),
  monPlugin()
]
```


### 🧭 Où définir un nouveau chemin d'alias ?

Dans la section `resolve.alias` :
```ts
resolve: {
  alias: {
    '@components': resolve(__dirname, 'src/components')
  }
}
```
Puis : 
``` import MyComponent from '@components/MyComponent.vue';```


### 🧪 Comment activer le mode bibliothèque dans `opensilex-front` ?

Décommente les lignes suivantes dans `vite.config.ts` :
```ts
lib: {
  entry: resolve(__dirname, 'src/lib/index.ts'),
  name: 'opensilex-front',
  fileName: (format) => `opensilex-front.${format}.js`,
  formats: ['es', 'umd']
}
```
Et ajoute un `export` explicite dans `src/lib/index.ts`.


### 🌍 Comment intégrer l’internationalisation (i18n) dans les `.vue` ?
```ts
VueI18nPlugin({
  defaultSFCLang: "yaml"
})
```
Cela permet de faire : 
```ts
<i18n lang="yaml">
en:
  hello: "Hello"
fr:
  hello: "Bonjour"
</i18n>
```

### 📝 Bonnes pratiques
- 🔒 N’incluez jamais `vue` dans vos bundles de librairies → elle doit être external.

- ✅ Utilisez base: '`/app/`' pour assurer une bonne gestion du chemin racine.

- 🧹 Gardez les plugins séparés selon les besoins (devtools pour l’app, pas pour les modules).
