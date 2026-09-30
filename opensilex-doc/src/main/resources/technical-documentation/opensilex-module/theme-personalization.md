# Technical documentation : [module front-end theme] What theme personalization allows and how to use it

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)              | OpenSILEX version   | Comment           |
|------------|------------------------|---------------------|-------------------|
| 30/09/2026 | yvan.roux@opensilex.fr | 1.5.0 Freaky Fossil | Document creation |

<!-- TOC -->
* [Technical documentation : [module front-end theme] What theme personalization allows and how to use it](#technical-documentation--module-front-end-theme-what-theme-personalization-allows-and-how-to-use-it)
  * [What theme personalization allows and how to use it](#what-theme-personalization-allows-and-how-to-use-it)
    * [CSS colors and styles](#css-colors-and-styles)
      * [create your own theme](#create-your-own-theme)
      * [Activate your theme](#activate-your-theme)
      * [Color personalization](#color-personalization)
      * [Styles personalization](#styles-personalization)
    * [Images personalization](#images-personalization)
    * [Looking for more personalization options](#looking-for-more-personalization-options)
  * [Technical behavior](#technical-behavior)
<!-- TOC -->

## What theme personalization allows and how to use it

Theme personalization allows you to customize the look of OpenSILEX front-end by overriding the CSS, colors and images.
You can only choose one theme at a time, and it should be activated in the config file to be used.

### CSS colors and styles

#### create your own theme

Below is the **minimal structure reference** for module theme override :

```bash
# module_name  => .e.g : opensilex-phis
# short_module_name  => .e.g : phis
{module_name}
├── front
│   ├── theme
│   │   └── {short_module_name}
│   │       ├── {short_module_name}.yml
│   │       └── main.css
```
Your `{short_module_name}.yml` config file should always start with `extend: "opensilex-front#opensilex"`. This instruction
allows using the default OpenSILEX theme, and to override only the variables and CSS you want to change. Skipping this instruction
will result in a complete override of the OpenSILEX theme, leading to a broken UI in future updates.

The config file should also at list reference one CSS file ine the `stylesheets` section. For our **minimal structure reference**
our `{short_module_name}.yml` file should at least look like this, with the `main.css` containing any CSS you want :
```yaml
extend: "opensilex-front#opensilex"
stylesheets:
  - main.css
```

#### Activate your theme

To activate your theme, add `theme: {module_name}#{short_module_name}` in the front section of your OpenSILEX config file.
For example, to load the phis theme, your config file should look like this :
```yaml
front:
  theme: opensilex-phis#phis
```

#### Color personalization

Many colors used by OpenSILEX default theme can be customized by just changing the value of the corresponding variable in the
`variables.scss` file.

in the example of our **minimal structure reference**, filling your `main.css` file with only the four lines below will result in a
theme without the light-green main color theme (used in buttons, header and more) but with a light-blue instead :
```css
:root {
    --main-color-theme: rgb(0 196 255 / 0.95);
    --main-color-theme-hover: rgb(0 149 255 / 0.95);
}
```
#### Styles personalization

The same way you can customize CSS style. Your CSS wil always be loaded after the OpenSILEX default CSS, so it will
have priority over it with the same selector level.

> ⚠️Please note that layout, classes and style can be changed in future releases. Overriding them may require some manual
> work when upgrading OpenSILEX.

### Images personalization

You can override any image in the `opensilex-front/front/theme/opensilex/images` directory by creating a new
image with the same name in your module's `front/theme/{short_module_name}/images` directory.

### Looking for more personalization options

If you have reached the limits of the theme personalization, you can still see [interface-personalization-how-to.md](interface-personalization-how-to.md)
to learn other ways to personalize the OpenSILEX front-end.

## Technical behavior

The `loadtheme` function of the `main.ts` file of the `opensilex-front` module loads the theme by adding it directly
in the DOM. CSS file is served by the `vuejs/theme/{moduleId}/{themeId}/style.css` route (code in `FrontAPI#getThemeCss`).