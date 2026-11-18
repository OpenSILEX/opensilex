<template>
  <ModalFormSelector
    ref="germplasmSelector"
    v-model:selected="germplasmUris"
    :modal-component="GermplasmModalList"
    :label="props.label"
    :placeholder="props.placeholder"
    :clearable="true"
    :multiple="true"
    :selected-in-json-format="props.editMode ? germplasm : null"
    :experiment="props.experimentUri"
    :limit="4"
    class="searchFilter"
    @clear="refreshGermplasmSelector"
    @hide="emit('hideSelector')"
    @shown="emit('shownSelector')"
  />
</template>

<script setup lang="ts">
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import { useTemplateRef } from "vue";
import GermplasmModalList from "@/components/germplasm/GermplasmModalList.vue";

interface GermplasmSelection {
  uri: string;
  name?: string;
  isDisabled?: boolean;
}

const props = withDefaults(
  defineProps<{
    editMode?: boolean;
    experimentUri?: string;
    placeholder?: string;
    label?: string;
  }>(),
  {
    editMode: false,
    experimentUri: undefined,
    placeholder: "component.germplasms.selector-with-filter.placeholder-multiple",
    label: "component.germplasms.selector-with-filter.label",
  },
);

const germplasmUris = defineModel<string[]>("germplasmUris")
const germplasm = defineModel<GermplasmSelection[]>()

const emit = defineEmits<{
  (event: "hideSelector"): void;
  (event: "shownSelector"): void;
}>();
const germplasmSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>(
  "germplasmSelector",
)

function refreshGermplasmSelector() {
  germplasmSelector.value?.refreshModalSearch();
}
</script>

<style scoped lang="scss">
</style>
