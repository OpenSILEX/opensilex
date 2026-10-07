<template>
  <span
      v-for="option in options"
      :key="option.id"
      class="switch-group__option"
  >
    <n-switch
        size="small"
        :value="selectedMap.get(option.id) ?? false"
        @update:value="(selected) => onUpdate(option.id, selected)"
    ></n-switch>
    <label
        class="switch-group__label"
        @click="() => toggle(option.id)"
    >{{ t(option.name) }}</label>
  </span>
</template>

<script setup lang="ts">
import {NSwitch} from "naive-ui";
import {ref, watch} from "vue";
import {useI18n} from "vue-i18n";

//#region Public
export interface SwitchOption {
  id: string;
  name: string;
}

const props = defineProps<{
  options: Array<SwitchOption>;
}>()

const model = defineModel<Array<string>>("value", {
  required: true,
  default: []
})
//#endregion

//#region Private
const {t} = useI18n()

const selectedMap = ref(new Map<string, boolean>())

watch(model, (value) => selectedMap.value = mapFromSelectedOptions(props.options, value), {immediate: true})

function toggle(id: string) {
  onUpdate(id, !selectedMap.value.get(id))
}

function onUpdate(id: string, value: boolean) {
  selectedMap.value.set(id, value)
  model.value = mapToSelectedOptions(selectedMap.value)
}

function mapFromSelectedOptions(options: Array<SwitchOption>, selected: Array<string>): Map<string, boolean> {
  const selectedSet = new Set(selected)
  const map = new Map<string, boolean>()
  for (const {id} of options) {
    map.set(id, selectedSet.has(id))
  }
  return map
}

function mapToSelectedOptions(map: Map<string, boolean>): Array<string> {
  return map.entries()
      .filter(([_, selected]) => selected)
      .map(([id, _]) => id)
      .toArray()
}
//#endregion
</script>

<style scoped>
.switch-group__option {
  margin-right: 3px;
}

.switch-group__label {
  cursor: pointer;
  margin: 0 5px;
}
</style>