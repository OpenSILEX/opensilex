<template>
  <opensilex-FormField
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="{ id }">
      <div class="tag-input-form">
        <!-- Current tags if at least one tag -->
        <div class="tags" v-if='localTags.length > 0'>
          <n-tag
            v-for="(t, i) in localTags"
            :key="t + '_' + i"
            closable
            :disabled="disabled"
            @close="removeTag(i)"
          >
            {{ t }}
          </n-tag>
        </div>

        <!-- Input + Add -->
        <div class="adder">
          <n-input
            :id="id"
            v-model:value="pending"
            :disabled="disabled"
            :placeholder="$t(placeholder || '')"
            @keyup.enter="addTag"
          />
          <n-button :disabled="disabled || !canAdd" @click="addTag">
            {{ $t('component.common.add') }}
          </n-button>
        </div>
      </div>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { NInput, NButton, NTag } from 'naive-ui'

type Props = {
  value?: string[]
  label?: string
  helpMessage?: string
  placeholder?: string
  required?: boolean
  disabled?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  value: () => [],
  label: '',
  helpMessage: '',
  placeholder: '',
  required: false,
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:value', v: string[]): void
  (e: 'change', v: string[]): void
}>()

const localTags = ref<string[]>([...props.value])
const pending = ref('')

watch(() => props.value, (elementValue) => {
  if (Array.isArray(elementValue)) localTags.value = [...elementValue]
}, { immediate: true })

const canAdd = computed(() => {
  const v = pending.value.trim()
  return v.length > 0 && !localTags.value.includes(v)
})

function addTag () {
  const v = pending.value.trim()
  if (!v || localTags.value.includes(v)) return
  localTags.value = [...localTags.value, v]
  pending.value = ''
  emit('update:value', localTags.value)
  emit('change', localTags.value)
}

// Keep all elelements except the one with the given index ("idx)")
// _ is a common  convention in TS, means: I know this parameter exists, but I don't use it.
function removeTag (idx: number) {
  localTags.value = localTags.value.filter((_, i) => i !== idx)
  emit('update:value', localTags.value)
  emit('change', localTags.value)
}
</script>

<style scoped>
.tag-input-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
}

.tags .n-tag {
background-color: #bfecdf;
/* background-color: #96a2ad */
}

.adder {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
