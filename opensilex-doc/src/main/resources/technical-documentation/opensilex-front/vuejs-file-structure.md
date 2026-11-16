# Technical documentation : [`Front`] `Vue file structure`

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)         | OpenSILEX version | Comment           |
|------------|-------------------|-------------------|-------------------|
| 2026-10-22 | Arnaud Charleroy  | develop (Vue 3)   | Document creation |

This document proposes a structure for the `.vue` files of the Vue 3 front, written with `<script setup lang="ts">`.
It replaces, for Vue 3 components, the class-based structure described in
[Vue component guidelines and template](./component-guidelines-template.md).

## Table of contents

<!-- TOC -->
* [Technical documentation : [`Front`] `Vue file structure`](#technical-documentation--front-vue-file-structure)
  * [Table of contents](#table-of-contents)
  * [Guidelines](#guidelines)
  * [Region order](#region-order)
  * [Event handlers](#event-handlers)
  * [Full example](#full-example)
<!-- TOC -->

## Guidelines

- All variables, methods and arguments are **always typed**, and the `any` type is never used.
- Similar elements are grouped together: data is not mixed with methods.
- Groups of similar elements are delimited with `//#region {Region name}` comments. IntelliJ and VSCode recognize them,
  which makes it easier to navigate through the structure of the file.
  - Short files can be limited to the `Public` and `Private` regions, as long as elements of the same kind are declared
    in the same place. Fine-grained regions pay off in large components, where it is easier to get lost.
- Methods and variables that are hard to understand are documented with a JSDoc comment (starting with `/**`, as in
  Javadoc).

## Region order

The recommended order of the regions is:

```
- Public
    - defineEmits()
    - props
    - defineModel() [v-model]
- Private
    - Plugins & services
    - Template refs
    - Data & Computed
    - Hooks & watchers
    - Event handlers
    - Methods
- defineExpose()
```

## Event handlers

The handler of an event _foo_ emitted by a child component _Child_ is declared as follows:

- Write a method `onFoo`, or `onChildFoo`, in the `Event handlers` region. It takes the payload of the event as
  parameter, with the right type.
- A one-line handler can be written inline in the template, as long as it stays clear. Declaring a method still
  improves readability.
- When the handler needs variables from the template scope in addition to the payload of the event, use an arrow
  function that calls the handler method.

## Full example

Below is an abstract example of a component following these guidelines. `OtherComponent`, `NestedComponent` and
`ExampleService` are fictional.

```vue
<template>
  <OtherComponent
    ref="otherComponent"
    @selected="onOtherComponentSelected"
    @updated="emit('toto', 'updated')"
  >
    <template #myslot="data">
      <NestedComponent
        @changed="(payload) => onNestedComponentChanged(payload, data)"
      />
    </template>
  </OtherComponent>
</template>

<script setup lang="ts">
import { computed, inject, onBeforeMount, ref, useTemplateRef, watch } from 'vue'
import { useStore } from 'vuex'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OpenSilexStore } from '@/models/Store'
import type { ExampleService } from 'opensilex-core/index'
import OtherComponent from '@/components/example/OtherComponent.vue'
import NestedComponent from '@/components/example/NestedComponent.vue'

//#region Public
const emit = defineEmits<{
  toto: [payload: string]
}>()

const props = withDefaults(
  defineProps<{
    exampleProp?: string
    exampleProp2?: boolean
  }>(),
  {
    exampleProp: 'test',
    exampleProp2: false,
  },
)

// v-model:value, a two-way binding with the parent component
const value = defineModel<string>('value')
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const exampleService = $opensilex.getService<ExampleService>('opensilex.ExampleService')
const store = useStore() as OpenSilexStore
//#endregion

//#region Template refs
const otherComponent = useTemplateRef<InstanceType<typeof OtherComponent>>('otherComponent')
//#endregion

//#region Data & Computed
const constData: string = 'readonly'
const writableData = ref<string>('writable and reactive')

const credentials = computed(() => store.state.credentials)
const readonlyComputed = computed<string>(() => constData)

const readWriteComputed = computed<string>({
  get: () => writableData.value.toUpperCase(),
  set: (newValue: string) => {
    writableData.value = newValue.toLowerCase()
  },
})
//#endregion

//#region Hooks & watchers
onBeforeMount(() => {
  doSomething()
})

watch(
  () => props.exampleProp,
  (newValue: string) => {
    if (newValue) {
      doSomething()
    }
  },
)
//#endregion

//#region Event handlers
function onOtherComponentSelected(payload: string): void {
  emit('toto', payload)
}

function onNestedComponentChanged(payload: string, data: { key: string }): void {
  writableData.value = data.key + ': ' + payload
}
//#endregion

//#region Methods
function show(): void {
  // ...
}

/**
 * A comment explaining this very complex method.
 */
function doSomething(): void {
  // ...
}
//#endregion

//#endregion

defineExpose({
  show,
})
</script>
```
