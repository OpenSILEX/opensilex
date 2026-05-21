<template>
  <opensilex-ModalForm
    v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID) && renderModalForm"
    ref="modalForm"
    :component="formComponent"
    :createTitle="t('DeviceModalForm.add')"
    :editTitle="t('DeviceModalForm.update')"
    icon="ik#ik-thermometer"
    modal-size="lg"
    :successMessage="successMessage"
    @onCreate="$emit('onCreate', $event)"
    @onUpdate="$emit('onUpdate', $event)"
  />
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'
import DTOConverter from '../../../models/DTOConverter'

import DeviceForm from './DeviceForm.vue'

import type { DevicesService } from 'opensilex-core/api/devices.service'
import type { DeviceCreationDTO, DeviceGetDetailsDTO } from 'opensilex-core/index'
import type { UserGetDTO } from '../../../../../../opensilex-security/front/src/lib'

const emit = defineEmits<{
  (e: 'onCreate', payload: any): void
  (e: 'onUpdate', payload: any): void
}>()

const store = useStore()
const { t } = useI18n()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<DevicesService>('opensilex.DevicesService')

const modalForm = ref<any>(null)
const renderModalForm = ref(false)
const formComponent = DeviceForm

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

function showCreateForm() {
  renderModalForm.value = true
  nextTick(() => {
    modalForm.value?.showCreateForm?.()
  })
}

async function showEditForm(uri: string) {
  try {
    const http =
      await service.getDevice(uri) as HttpResponse<OpenSilexResponse<DeviceGetDetailsDTO>>

    const device = http.response.result
    const publisher: UserGetDTO = device.publisher

    renderModalForm.value = true

    nextTick(() => {
      const form = modalForm.value?.getFormRef?.()

      form?.readAttributes?.(device.metadata)
      form?.typeSwitch?.(device.rdf_type, true)

      const editDto =
        DTOConverter.extractURIFromResourceProperties<DeviceGetDetailsDTO, DeviceCreationDTO>(device)

      editDto.publisher = publisher

      modalForm.value?.showEditForm?.(editDto)
    })
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

function successMessage(device: DeviceCreationDTO): string {
  return `${t('DeviceModalForm.name')} ${device.name} `
}

defineExpose({
  showCreateForm,
  showEditForm
})
</script>

<i18n>
en:
    DeviceModalForm:
        title: Devices
        description: Manage Devices
        add: Add device
        update: Update device
        delete: Delete device
        devices-help: Devices include objects, machines, electrical, electronic or mechanical devices, etc.
        name: The device
fr:
    DeviceModalForm:
        title: Appareils
        description: Gestion des appareils
        add: Ajouter un appareil
        update: Editer un appareil
        delete: Supprimer un appareil
        devices-help: Les appareils sont des objets, machines, dispositifs électriques, électroniques, mécaniques, etc.
        name: L'appareil
</i18n>