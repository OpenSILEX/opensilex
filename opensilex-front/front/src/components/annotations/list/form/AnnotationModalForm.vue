<template>
  <AnnotationForm
    ref="formRef"
    :createTitle="'component.annotation.add'"
    :editTitle="'component.annotation.edit'"
    @onCreate="emit('onCreate', $event)"
    @onUpdate="emit('onUpdate', $event)"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AnnotationForm from './AnnotationForm.vue'

const emit = defineEmits<{
  (e: 'onCreate', uri: any): void
  (e: 'onUpdate', uri: any): void
}>()

const formRef = ref<InstanceType<typeof AnnotationForm> | null>(null)

function showCreateForm(targetsArg: string[] = []) {
  formRef.value?.showCreateForm(targetsArg)
}

function showEditForm(form: any) {
  formRef.value?.showEditForm(form)
}

defineExpose({
  showCreateForm,
  showEditForm
})
</script>
