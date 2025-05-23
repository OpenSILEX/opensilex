<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-primary"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#pencil-alt"
  ></opensilex-Button>
</template>

<script lang="ts">
import { ref, defineProps, }  from 'vue';

const props = defineProps<{
  label: string;
  small?: boolean;
  disabled?: boolean;
}>();

export default {
  props,
};

</script>

<style scoped lang="scss">
</style>

