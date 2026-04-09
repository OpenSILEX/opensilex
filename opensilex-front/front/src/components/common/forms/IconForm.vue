<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <n-select
        :options="icons"
        v-model="value"
      >
      </n-select>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {computed, inject, nextTick, onMounted, ref, useTemplateRef} from "vue";
import {NSelect} from "naive-ui";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")

const observer = ref<IntersectionObserver>();
const limit = ref(20);
const search = ref("");
const icons = computed(() => {
  return opensilex.getSelectIconIDs().map(({id, label}) => ({
    label,
    value: id,
  }))
});

// const filtered = computed(() => icons.value.filter(icon => icon.id.includes(search.value)));
// const paginated = computed(() => filtered.value.slice(0, limit.value));

const value = defineModel('value');

const props = defineProps<{
  label: string,
  helpMessage: string,
  placeholder: string,
  required: boolean,
  disabled: boolean,
  rules: string | Function
}>();

//   withPopper(dropdownList, component, { width }) {
//     /**
//      * We need to explicitly define the dropdown width since
//      * it is usually inherited from the parent with CSS.
//      */
//     dropdownList.style.width = width;
//
//     /**
//      * Here we position the dropdownList relative to the $refs.toggle Element.
//      *
//      * The 'offset' modifier aligns the dropdown so that the $refs.toggle and
//      * the dropdownList overlap by 1 pixel.
//      *
//      * The 'toggleClass' modifier adds a 'drop-up' class to the Vue Select
//      * wrapper so that we can set some styles for when the dropdown is placed
//      * above.
//      */
//     const popper = createPopper(component.$refs.toggle, dropdownList, {
//       placement: "top",
//       modifiers: [
//         {
//           name: "offset",
//           options: {
//             offset: [0, -1]
//           }
//         },
//         {
//           name: "toggleClass",
//           enabled: true,
//           phase: "write",
//           fn({ state }) {
//             component.$el.classList.toggle(
//               "drop-up",
//               state.placement === "top"
//             );
//           }
//         }
//       ]
//     });
//
//     /**
//      * To prevent memory leaks Popper needs to be destroyed.
//      * If you return function, it will be called just before dropdown is removed from DOM.
//      */
//     return () => popper.destroy();
//   }
// }
</script>

<style scoped lang="scss">
::v-deep .vs__selected {
  white-space: break-spaces;
}

.open-indicator {
  color: rgb(204, 204, 204);
}

.open-indicator:hover {
  cursor: pointer;
  color: rgb(97, 97, 97);
}

::v-deep .open-indicator svg {
  font-size: 16px;
}

::v-deep .v-select-unselect {
  color: rgb(204, 204, 204);
}

::v-deep .v-select-unselect:hover {
  color: #e53935;
  cursor: pointer;
}
</style>

