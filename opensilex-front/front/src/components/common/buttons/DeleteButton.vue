<template>
    <opensilex-Button
      :label="label"
      :small="small"
      :disabled="disabled"
      variant="outline-danger"
      icon="fa#trash-alt"
      @click="openModal"
    />

    <!-- Modal Bootstrap 5 -->
    <teleport to="body">
    <div
      class="modal fade delete-confirm-modal"
      tabindex="-1"
      ref="modalRef"
      aria-labelledby="deleteModalLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-sm modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-body">
            {{ t('component.common.delete-confirmation') }}
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-secondary"
              data-bs-dismiss="modal"
              @click="closeModal"
            >
              {{ t('component.common.cancel') }}
            </button>
            <button
              type="button"
              class="btn btn-danger"
              @click="confirmDelete"
            >
              {{ t('component.common.delete') }}
            </button>
          </div>
        </div>
      </div>
    </div>
    </teleport>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Modal } from 'bootstrap'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  label: string
  small: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

const { t } = useI18n()

const modalRef = ref<HTMLElement | null>(null)
let modalInstance: Modal | null = null

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value, {
      backdrop: 'static',
      keyboard: false,
    })
  }
})

function openModal() {
  modalInstance?.show()

  setTimeout(() => {
    // force z-index modal au dessus pour les modales supperpo
    if (modalRef.value) (modalRef.value as any).style.zIndex = '10050'

    // force z-index du backdrop Bootstrap (celui créé en dernier)
    const backdrops = document.querySelectorAll('.modal-backdrop')
    const last = backdrops[backdrops.length - 1] as HTMLElement | undefined
    if (last) {
      last.classList.add('delete-confirm-backdrop')
      last.style.zIndex = '10040'
    }
  })
}

function closeModal() {
  modalInstance?.hide()
}

function confirmDelete() {
  modalInstance?.hide()
  emit('click')
}
</script>

<style scoped lang="scss">
</style>
