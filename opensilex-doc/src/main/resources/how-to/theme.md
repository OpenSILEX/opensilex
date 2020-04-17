Theme
=============

# How to create a module theme 

```bash
# full-module-name  => .e.g : inrae-sixtine
# Module_name  => .e.g : Sixtine
# module_name  => .e.g : sixtine
{full-module-name} # module
├── front # front
│   ├── babel.config.js # translation config
│   ├── package.json # module javascript packages description
│   ├── src # javascript sources
│   │   ├── components # vue components
│   │   │   └── layout
│   │   │       ├── {Module_name}FooterComponent.vue
│   │   │       ├── {Module_name}HeaderComponent.vue
│   │   │       ├── {Module_name}HomeComponent.vue
│   │   │       ├── {Module_name}LoginComponent.vue
│   │   │       └── {Module_name}MenuComponent.vue
│   │   ├── index.ts # register vue components
│   │   ├── lang # lang translation
│   │   │   ├── {module_name}-en.json
│   │   │   └── {module_name}-fr.json
│   │   ├── lib # need to build archive
│   │   └── shims-vue.d.ts # ??
│   ├── theme # theme files imgs, scss variables, fonts etc...
│   │   └── {module_name}
│   │       ├── {module_name}.yml
│   │       ├── fonts
│   │       ├── images
│   │       └── variables.scss
│   ├── tsconfig.json # typescript config
│   ├── vue.config.js # vue config
│   └── yarn.lock # yarn packages
```

# Layout theme example
```bash
{full-module-name} # module
├── front # front
│   ├── src # javascript sources
│   │   ├── components # vue components
│   │   │   └── layout
│   │   │       ├── {Module_name}FooterComponent.vue
│   │   │       ├── {Module_name}HeaderComponent.vue
│   │   │       ├── {Module_name}HomeComponent.vue
│   │   │       ├── {Module_name}LoginComponent.vue
│   │   │       └── {Module_name}MenuComponent.vue
```