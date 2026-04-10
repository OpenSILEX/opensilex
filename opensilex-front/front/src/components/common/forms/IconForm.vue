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
        v-model:value="value"
        filterable
      >
      </n-select>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {h, inject, onMounted, ref, VNodeChild} from "vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {NSelect, SelectOption} from "naive-ui";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")

const icons = ref<Array<{
  value: string,
  label: string | ((option: SelectOption) => VNodeChild),
}>>( []);

onMounted(() => {
  icons.value = opensilex.getSelectIconIDs().map(({id, iconName}) => ({
    value: id,
    // There is no slot at the moment for select options, so we provide a render function
    label: (option: SelectOption) => h('div', [h(FontAwesomeIcon, {icon: iconName, style: { 'margin-right': '10px' }}), option.value]),
  }))
});

const value = defineModel<string>('value');

defineProps<{
  label: string,
  helpMessage: string,
  placeholder: string,
  required: boolean,
  disabled: boolean,
  rules: string | Function
}>();
</script>

<style scoped lang="scss">
</style>

