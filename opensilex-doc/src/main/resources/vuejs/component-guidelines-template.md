# Vue component guidelines and template

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2024-08-29 | Valentin Rigolle | 1.3.3             | Document creation |

This file describes coding guidelines and good practices that should be applied to Vue components in OpenSILEX.
You can find a full example at the end of this file, as well as an empty template to copy / paste into your components.

<!-- TOC -->
* [Vue component guidelines and template](#vue-component-guidelines-and-template)
  * [Guidelines](#guidelines)
    * [Typing](#typing)
    * [Visibility](#visibility)
    * [Mutability](#mutability)
    * [Documentation](#documentation)
    * [Structure](#structure)
    * [Event emits](#event-emits)
    * [Event handlers](#event-handlers)
    * [Watchers](#watchers)
  * [Full example](#full-example)
  * [Empty template](#empty-template)
  * [Existing component migration](#existing-component-migration)
<!-- TOC -->

## Guidelines

### Typing

All variables, methods and method parameters should **always** be annotated with the **correct type**, i.e. never the
`any` type.

Here are some non-trivial types :

```ts
$router: VueRouter;
$route: Route;
$store: OpenSilexStore;
$t: typeof VueI18n.prototype.t;
```

### Visibility

All variables and methods should be annotated with a visibility modifier (`private` or `public`), in order to know what
should be accessible from the outside.

- All variables should be `private`.
- Methods should be `private` by default. The only public methods are those which the parent component needs to call in
  order to do a specific task (e.g. `showCreateForm` in the modal windows).

### Mutability

- Variables should be immutable when necessary (using the `readonly` keyword) :
  - Plugins like `$opensilex` are always `readonly` (because they are injected by Vue).
  - `@Prop`s are always `readonly`. On the contrary, `@PropSync`s are always mutable.
  - `@Ref`s are always `readonly` (they are also injected by Vue).
  - Any constant data should be `readonly`.

### Documentation

Methods and variables that are difficult to understand should be annotated with a JSDoc comment (starting with
`/**`).

It is also a good practice to comment the props, events and public methods (i.e. parts of the component that are
accessible from a parent component), even with a short sentence.

### Structure

Similar elements like props, data, methods, etc. should be regrouped in **regions**. The regions are defined using
special comments that are recognized by IDEs :

```ts
//#region {name of the region}

//#endregion ({name of the region})
```

> The name of the region after the `#endregion` marker is optional, however it is recommended to systematically fill it
> to increase the readability when a lot of regions are present.

> For VSCode, plugins like [Region Marker](https://marketplace.visualstudio.com/items?itemName=txava.region-marker)
> allow to use this feature more easily.

Below is a proposal for a region structure that can be used in every component :

- Contract/External (= what is exposed to the parent component)
  - Props (including PropSyncs)
  - Event emits
  - Public methods
- Implementation/Internal (= what is only used internally)
  - Plugins & services
  - Refs
  - Data
  - Computed
  - Hooks
  - Event handlers and watchers
  - Private methods

It is not necessary that you strictly follow this structure, as long as you regroup similar elements into regions.

### Event emits

For each event type the component can emit, a private method is defined in the `Event emits` region. The method is
named after the event prefixed with "emit" and takes the event parameter annotated with the correct type.

Anywhere else in the code, the specific event emit method must be used instead of the built-in `$emit` method.

Using this structure allows us to easily know the event names and parameters a component can emit. It also minimizes the
risk of emitting an incorrect event name, or emitting and event with a parameter of the wrong type.

For example, in my `ColorPicker` component, I can emit two events : `colorPicked` when the user has selected a color,
and `closed` when the component is closed. So, I will define two methods (in this example, a color is represented as
an array of numbers).

```ts
export default class ColorPicker extends Vue {
  //...

  //#region Event emits
  private emitColorPicked(color: Array<number>) {
    this.$emit("colorPicked", color);
  }

  private emitClosed() {
    this.$emit("closed");
  }
  //#endregion (Event emits)

  //...

  //#region Private methods
  private close() {
    //Do stuff...
    this.emitClosed(); //using emitClosed() instead of $emit("closed")
  }
  //#endregion (Private methods)
}
```

### Event handlers

Event handlers of child components can be written in two ways :

- If the handler uses event parameters, a private method is defined in the `Event handlers` region. The method is 
  named after the event, prefixed with "on" or "on{ChildComponent}" if it is necessary to avoid confusion between 
  multiple child components. The method must only be called as an event handler and not by any other method.
- If the handler doesn't use any event parameter, it can be defined inline in the template.

In my `ColorPicker` example, I have three children : a `ColorInput`, a `ValidateButton` and a `CloseButton`. The
`ColorInput` emits an `input` event, while both buttons emit a `click` event.

- The `input` event from `ColorInput` is handled by a `onColorInput` method that takes the selected color as parameter.
  Because of the parameter, this is the only way to handle the event.
- The `click` event of the validation button is handled in a similar way, with no event parameter.
- The `click` event of the close button directly calls the `close` method and does not define a method in the
  `Event handlers` region. This is another way of handling this case.

> As you can see, events with no parameter can be handled in two different ways. We recommend the following :
> 
> - If the method handling the event might be used elsewhere (such as the `close` method in this example), then the
>   handler should be inline.
> - If you are certain that the method will only be used by the event, then it can be its own event handler method. In
>   this example, that would mean that `onValidateButtonClicked` performs specific logic that is not reused in the
>   component.

```vue
<template>
  <div>
    <ColorInput
        @input="onColorInput"
    ></ColorInput>
    <ValidateButton
        @click="onValidateButtonClicked"
    >
    </ValidateButton>
    <CloseButton
        @click="close()"
    ></CloseButton>
  </div>
</template>
<script lang="ts">
  import Component from "vue-property-decorator";

  @Component
  export default class ColorPicker extends Vue {
    //...

    //#region Event handlers and watchers
    private onColorInput(color: Array<number>) {
      //Do stuff...
    }
    
    private onValidateButtonClicked() {
      //Do stuff that is only useful when the validate button is clicked...
    }
    //#endregion (Event handlers and watchers)
    
    //...
    
    //#region Private methods
    private close() {
      //Do stuff that is useful when the close button is clicked but also can be called from elsewhere...
    }
    //#endregion (Private methods)
    
    //...
  }
</script>
```

### Watchers

Watchers are special methods that are called when a reactive variable changes. As they are similar to the concept of 
event handlers, they are located in the same region.

Watcher methods should be named `on{Variable}Change` and marked private.

```ts
export default class ColorPicker extends Vue {
  //...

  //#region Data
  private color: Array<number> = [0, 0, 0];
  //#endregion (Data)

  //...

  //#region Event handlers and watchers
  @Watch("color")
  private onColorChange(newValue: Array<number>, oldValue: Array<number>) {
    //Do stuff...
    console.debug("Internal color value changed", newValue);
    console.debug("Old value was", oldValue);
  }
  //#endregion (Event handlers and watchers)

  //...
}
```

## Full example

```vue

<template>
  <div>
    <ColorInput
            :ref="colorInput"
            @input="onColorInput"
    ></ColorInput>
    <ValidateButton
            @click="onValidateButtonClicked"
    >
    </ValidateButton>
    <CloseButton
            @click="close()"
    ></CloseButton>
  </div>
</template>
<script lang="ts">
  import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";

  /**
   * A modal color input.
   */
  @Component
  export default class ColorPicker extends Vue {
    //#region Contract
    //#region Props
    /**
     * Initial value for the selected color.
     *
     * @private
     */
    @Prop({default: [0, 0, 0]})
    private readonly initialColor: Array<number>;

    /**
     * Synchronized value for the selected color.
     *
     * @private
     */
    @PropSync("selection")
    private selectedColor: Array<number>;
    //#endregion (Props)
    
    //#region Event emits

    /**
     * Event "colorPicked". Emitted when the selected color changes.
     *
     * @param color The new selected color.
     * @private
     */
    private emitColorPicked(color: Array<number>) {
      this.$emit("colorPicked", color);
    }

    /**
     * Event "closed". Emitted when the modal color picker is closed.
     *
     * @private
     */
    private emitClosed() {
      this.$emit("closed");
    }
    //#endregion (Event emits)
    
    //#region Public methods
    /**
     * Sets the selected color.
     *
     * @param color The new color to select.
     */
    public setColor(color: Array<number>) {
      this.color = color;
    }
    //#endregion (Public methods)
    //#endregion (Contract)
    
    //#region Implementation
    //#region Plugins & services
    private readonly $opensilex: OpenSilexVuePlugin;
    private exampleService: ExampleService;
    //#endregion (Plugins & services)
    
    //#region Refs
    @Ref("colorInput")
    private readonly colorInput: ColorInput;
    //#endregion (Refs)
    
    //#region Data
    private color: Array<number> = [0, 0, 0];
    //#endregion (Data)
    
    //#region Computed
    private colorHex(): string {
      return "#52FF1A";
    }
    //#endregion (Computed)
    
    //#region Hooks
    private created() {
      this.exampleService = this.$opensilex.getService("ExampleService");
    }
    //#endregion (Hooks)
    
    //#region Event handlers and watchers
    @Watch("color")
    private onColorChange(newValue: Array<number>, oldValue: Array<number>) {
      console.debug("Internal color value changed", newValue);
      console.debug("Old value was", oldValue);
    }
    
    private onColorInput(color: Array<number>) {
      //do stuff...
    }

    private onValidateButtonClicked() {
      //do stuff...
      this.close();
    }
    //#endregion (Event handlers and watchers)
    
    //#region Private methods
    private close() {
      //Do stuff...
      this.emitClosed(); //using emitClosed() instead of $emit("closed")
    }
    //#endregion (Private methods)
    //#endregion (Implementation)
  }
</script>
```

## Empty template

You can copy and paste this template when creating a new component. The class name should be the same as the
file.

```vue
<template>
  
</template>

<script lang="ts">
  import Component from "vue-property-decorator";

  @Component
  export default class MyComponent extends Vue {
    //#region Contract
    //#region Props
    
    //#endregion (Props)
    //#region Event emits
    
    //#endregion (Event emits)
    //#region Public methods
    
    //#endregion (Public methods)
    //#endregion (Contract)
    
    //#region Implementation
    //#region Plugins & services

    //#endregion (Plugins & services)
    //#region Refs

    //#endregion (Refs)
    //#region Data

    //#endregion (Data)
    //#region Computed

    //#endregion (Computed)
    //#region Hooks

    //#endregion (Hooks)
    //#region Event handlers and watchers

    //#endregion (Event handlers and watchers)
    //#region Private methods

    //#endregion (Private methods)
    //#endregion (Implementation)
  }
</script>

<style lang="scss" scoped>
  
</style>

<i18n>
  
</i18n>
```

## Existing component migration

OpenSILEX currently has a lot of already existing Vue components that DO NOT follow the guidelines described in this 
file. We DO NOT aim to make every existing component follow these guidelines. However, when working on such 
components, feel free to update them so that they follow the guidelines (if you have the time and motivation for it).

> As specified above, new components SHOULD always follow these guidelines.