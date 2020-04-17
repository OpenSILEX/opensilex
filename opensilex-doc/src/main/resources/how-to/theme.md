Theme
=============

### Modify a theme

In this directory you can modify theme style or override existing one.

```bash
│   ├── theme # theme files imgs, scss variables, fonts etc...
│   │   └── {short_module_name}
│   │       ├── {short_module_name}.yml
│   │       ├── fonts
│   │       ├── images
│   │       └── variables.scss
```

### Modify layout Component

In this directory you can extend layout component.

```bash
{module_name} # module
├── front # front
│   ├── src # javascript sources
│   │   ├── index.ts # register vue components
│   │   ├── components # vue components
│   │   │   └── layout
│   │   │       ├── {Module_name}FooterComponent.vue
│   │   │       ├── {Module_name}HeaderComponent.vue
│   │   │       ├── {Module_name}HomeComponent.vue
│   │   │       ├── {Module_name}LoginComponent.vue
│   │   │       └── {Module_name}MenuComponent.vue
```
