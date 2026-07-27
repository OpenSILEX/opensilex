<template>
  <div class="container-fluid">
    <opensilex-PageActions
      class="pageActions"
      v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
    >
      <opensilex-CreateButton
        :label="t('Device.add')"
        @click="deviceFormRef?.showCreateForm?.()"
        class="createButton"
      />

      <opensilex-CreateButton
        :label="t('Device.import')"
        @click="showCsvForm"
        class="createButton"
      />

      <h5>
        <n-tooltip trigger="hover" placement="top">
        <template #trigger>
            <font-awesome-icon icon="question-circle" class="devicesHelp" />
        </template>
        {{ t('Device.devices-help') }}
        </n-tooltip>
      </h5>

      <DeviceForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
        ref="deviceFormRef"
        @onCreate="displayAfterCreation"
        :createTitle="t('component.device.add')"
        :editTitle="t('component.device.update')"
      />

      <DeviceCsvForm
        v-if="renderCsvForm"
        ref="csvFormRef"
        @csvImported="deviceListRef?.refresh?.()"
      />
    </opensilex-PageActions>

    <opensilex-PageContent>
      <DeviceList
          ref="deviceListRef"
          @onUpdate="onDeviceListUpdate"
      />
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { NTooltip } from 'naive-ui'
// @ts-ignore
import type { DeviceCreationDTO } from 'opensilex-core/index'
import DeviceForm from "@/components/devices/form/DeviceForm.vue";
import DeviceCsvForm from "@/components/devices/csv/DeviceCsvForm.vue";
import DeviceList from "@/components/devices/DeviceList.vue";

const store = useStore()
const router = useRouter()
const { t } = useI18n();

const renderCsvForm = ref(false)

const deviceFormRef = ref<any>(null)
const csvFormRef = ref<any>(null)
const deviceListRef = ref<any>(null)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

function showCsvForm() {
  renderCsvForm.value = true
  nextTick(() => {
    csvFormRef.value?.show?.()
  })
}

function displayAfterCreation(device: DeviceCreationDTO) {
  const uri = device?.uri
  if (!uri) return

  router.push({
    path: '/device/details/' + encodeURIComponent(uri)
  })
}

function onDeviceListUpdate(dto: any): void {
  const copydto = JSON.parse(JSON.stringify(dto));
  deviceFormRef.value.showEditForm(copydto);
}
</script>

<style scoped lang="scss">
.createButton {
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
  margin-right: 5px;
}

.devicesHelp {
  font-size: 1.3em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
   margin-top: -10px;
}

</style>

<i18n>
en:
  Device:
    title: Devices
    description: Manage Devices
    add: Add device
    update: Update device
    delete: Delete device
    devices-help: Devices include objects, machines, electrical, electronic or mechanical devices, etc.
    import: CSV Import
    
fr:
  Device:
    title: Appareils
    description: Gestion des appareils
    add: Ajouter un appareil
    update: Editer un appareil
    delete: Supprimer un appareil
    devices-help: Les appareils sont des objets, machines, dispositifs électriques, électroniques, mécaniques, etc.
    import: Import CSV
</i18n>