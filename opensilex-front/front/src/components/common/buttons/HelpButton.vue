<template>
  <opensilex-Button
    @click=" () => emit('click')"
    :label="label"
    class="helpButton"
    :variant="variant"
    :disabled="disabled"
    :small="small"
    icon="fa#question"
  />
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from "vue";

defineProps<{
  label?: string;
  small?: boolean;
  disabled?: boolean;
  variant: { type: String, default: "outline-info" }
}>();

const emit = defineEmits(["click"]);

</script>

<style scoped lang="scss">

.helpButton {
  color: #00A28C;
  border: none;
  height: 30px;
  padding: .375rem .75rem
}
  
.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}
</style>
