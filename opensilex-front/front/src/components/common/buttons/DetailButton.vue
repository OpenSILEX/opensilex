<template>
  <Button
    v-if="!automaticToogle"
    @click="$emit('click', {})"
    variant="outline-success"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template #icon>
      <opensilex-Icon v-if="!detailVisible" icon="fa#eye" />
      <opensilex-Icon v-else icon="fa#eye-slash" />
    </template>
  </Button>

  <Button
    v-else
    @click="automaticToogleClick"
    variant="outline-success"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template #icon>
      <opensilex-Icon v-if="automaticSwitch" icon="fa#eye" />
      <opensilex-Icon v-else icon="fa#eye-slash" />
    </template>
  </Button>
</template>

<script lang="ts" setup>
import { ref }  from 'vue';
import Button from "@/components/common/buttons/Button.vue";

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
