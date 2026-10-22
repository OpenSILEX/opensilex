<template>
  <ModalFormSelector
      ref="germplasmSelector"
      :label="label"
      :placeholder="props.placeholder"
      v-model:selected="germplasmUris"
      modalComponent="opensilex-GermplasmModalList"
      :clearable="true"
      :multiple="true"
      :selectedInJsonFormat="props.editMode ? germplasm : null"
      :experiment="experimentUri"
      @clear="refreshGermplasmSelector"
      :limit="4"
      @hide='$emit("hideSelector")'
      @shown='$emit("shownSelector")'
      class="searchFilter"
  ></ModalFormSelector>
</template>

<script setup lang="ts">
import ModalFormSelector from "@/components/variables/form/ModalFormSelector.vue";
import {useTemplateRef} from "vue";
import {SelectableItem} from "@/components/common/views/ModalListBuilder.vue";

const germplasmSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>('germplasmSelector')

const props = withDefaults(defineProps<{
  editMode: boolean
  experimentUri: string
  placeholder: string
  label: string

}>(), {
  placeholder: "component.germplasms.selector-with-filter.placeholder-multiple",
  label: "component.germplasms.selector-with-filter.label"
});

const germplasmUris = defineModel<string[]>("germplasmUris")
const germplasm = defineModel<SelectableItem[]>()

const emit = defineEmits<{
  (event: "hideSelector"): void;
  (event: "shownSelector"): void;
}>();

function refreshGermplasmSelector() {
  germplasmSelector.value.refreshModalSearch();
}

</script>

<style scoped lang="scss">
</style>
