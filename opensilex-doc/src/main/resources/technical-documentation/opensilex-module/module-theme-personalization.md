Theme
=============

### Modify a logo

There are two ways to modify logo :

First way modifiy Login component.

1. Find component
```bash
{module_name} # module
├── front # front
│   ├── src # javascript sources 
│   │   ├── components # vue components
│   │   │   └── layout 
│   │   │       ├── {Module_name}LoginComponent.vue 
```

2. Change image path in the html tag used to define the background image

```html
    <slot
    class="lavalite-bg"
    v-bind:style="{ 'background-image': 'url(' + $opensilex.getResourceURI('images/opensilex-login-bg.jpg') + ')' }"
    >
```

### Set custom application name

You can update the displayed name of your application (OpenSILEX by default) by just changing your `opensilex.yml` configuration.

```yaml
front:
    applicationName: your_custom_application_name
```


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
