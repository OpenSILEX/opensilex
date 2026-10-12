<template>
  <OVLButton
    v-if="!automaticToogle"
    @click="$emit('click', {})"
    variant="outline-success"
    :small="small"
    :label="t(label)"
    :disabled="disabled"
  >
    <template #icon>
      <OVLIcon v-if="!detailVisible" icon="fa#eye" />
      <OVLIcon v-else icon="fa#eye-slash" />
    </template>
  </OVLButton>

  <OVLButton
    v-else
    @click="automaticToogleClick"
    variant="outline-success"
    :small="small"
    :label="t(label)"
    :disabled="disabled"
  >
    <template #icon>
      <OVLIcon v-if="automaticSwitch" icon="fa#eye" />
      <OVLIcon v-else icon="fa#eye-slash" />
    </template>
  </OVLButton>
</template>

<script lang="ts" setup>
import { ref }  from 'vue';
import { useI18n } from "vue-i18n";
import { OVLButton, OVLIcon } from "opensilex-vue-library";

const { t } = useI18n();

const props = defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
  detailVisible?: boolean;
  automaticToogle?: boolean;
}>();

const emit = defineEmits<{
  (e: 'click', payload: {}): void;
}>();

const automaticSwitch = ref(true);

function automaticToogleClick() {
  emit('click', {});
  automaticSwitch.value = !automaticSwitch.value;
}
</script>

<style scoped lang="scss">
</style>
