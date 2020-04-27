OpenSilex Modules
=================

# Module architecture

```bash
# full_module_name  => .e.g : inrae-sixtine
# Module_name  => .e.g : Sixtine
# module_name  => .e.g : sixtine
{full_module_name} # module
├── front # front
│   ├── babel.config.js # translation config
│   ├── package.json # module javascript packages description
│   ├── src # javascript sources
│   │   ├── components # vue components
│   │   │   └── layout # extends or change layout (optional)
│   │   ├── index.ts # register vue components
│   │   ├── lang # lang translation override or add new key
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