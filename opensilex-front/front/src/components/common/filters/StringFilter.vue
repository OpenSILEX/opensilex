<template>
  <form ref="formRef" @submit.prevent>
    <div class="input-group input-group-sm">
      <opensilex-FormInputLabelHelper
        class="mt-2 mr-2"
        v-if="label"
        :label="label"
        :helpMessage="helpMessage"
        :labelFor="id"
      />

      <input
        :id="id"
        class="form-control filter"
        :disabled="disabled"
        :value="filterValue"
        :type="type"
        :placeholder="t(placeholder)"
        @input="onInput"
        @keyup.enter="onEnter"
      />

      <button
        class="btn btn-outline-light clear-btn"
        type="button"
        @click="clear"
      >
        <opensilex-Icon icon="fa#times" />
      </button>
    </div>
  </form>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted, inject } from 'vue';
import { useI18n } from 'vue-i18n';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

// Props
const {
  filter,
  placeholder,
  label,
  helpMessage,
  disabled = false,
  type = 'text',
  max,
  min,
  debounce = 0,
  lazy = true,
} = defineProps<{
  filter: string;
  placeholder: string;
  label?: string;
  helpMessage?: string;
  disabled?: boolean;
  type?: string;
  max?: number;
  min?: number;
  debounce?: number;
  lazy?: boolean;
}>();


// Emits
const emit = defineEmits<{
  (e: 'update:filter', value: string): void;
  (e: 'update', value: string): void;
  (e: 'handlingEnterKey'): void;
}>();

// i18n
const { t } = useI18n();

// Refs & State
const formRef = ref<HTMLFormElement | null>(null);
const id = ref<string>('');
const filterValue = ref(filter);

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex');

onMounted(() => {
  id.value = $opensilex?.generateID() || `input-${Math.random().toString(36).substr(2, 9)}`;
  filterValue.value = filter;
});

watch(
  () => filter,
  (newVal) => {
    filterValue.value = newVal;
  }
);

// Gestion de l'input
const onInput = (event: Event) => {
  const value = (event.target as HTMLInputElement).value;
  if (value !== filterValue.value) {
    if (type === 'number') {
      let valid = true;
      const numberValue = parseInt(value);

      if (!Number.isNaN(numberValue)) {
        if (min !== undefined) {
          valid = numberValue >= min;
        }
        if (max !== undefined) {
          valid = valid && numberValue <= max;
        }
      } else if (value !== '') {
        valid = false;
      }

      if (valid || value === '') {
        filterValue.value = value;
        emit('update:filter', value);
        emit('update', value);
      }
    } else {
      filterValue.value = value;
      emit('update:filter', value);
      emit('update', value);
    }
  }
};

// Bouton clear
const clear = () => {
  formRef.value?.reset();
  filterValue.value = '';
  emit('update:filter', '');
  emit('update', '');
};

// Touche Entrée
const onEnter = () => {
  emit('handlingEnterKey');
};
</script>


<style scoped lang="scss">
.filter {
  font-size: 13px;
  border-right: none;
}

.clear-btn {
  color: rgb(229, 227, 227) !important;
  border-color: rgb(229, 227, 227) !important;
  border-left: none !important;
}

.clear-btn:hover,
.clear-btn:focus,
.clear-btn:active {
  box-shadow: none !important;
  color: rgb(229, 57, 53) !important;
  background-color: transparent !important;
  border-left: none !important;
}

input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type=number] {
  -moz-appearance: textfield;
}
</style>
