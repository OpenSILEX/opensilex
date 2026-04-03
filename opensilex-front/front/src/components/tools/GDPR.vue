<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <opensilex-Card icon="bi#bi-info-circle" :label="t('GDPR.title-description')">
        <template #body>
          <h3 v-if="!documentIsSetInTheConfig">
            {{ t("GDPR.no-config-file") }}
          </h3>

            <iframe
            v-else-if="documentIsAvailable"
            id="pdf-container"
            :src="encodedPdfUrl"
            />

          <h3 v-else>
            {{ t("GDPR.error-document") }}
          </h3>
        </template>
      </opensilex-Card>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, watch, onMounted, onBeforeUnmount } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { SecurityService } from 'opensilex-security/api/security.service'
import { useI18n } from 'vue-i18n'

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()

const documentIsSetInTheConfig = ref(true)
const documentIsAvailable = ref(true)
const encodedPdfUrl = ref('')

const currentLanguage = computed(() => opensilex.getLang())

async function loadPdf() {
  documentIsSetInTheConfig.value = opensilex.getConfig().gdprFileIsConfigured
  documentIsAvailable.value = true

  if (!documentIsSetInTheConfig.value) {
    if (encodedPdfUrl.value) {
      URL.revokeObjectURL(encodedPdfUrl.value)
      encodedPdfUrl.value = ''
    }
    return
  }

  try {
    const language = { language: currentLanguage.value }
    const securityService =
      opensilex.getService<SecurityService>('opensilex.SecurityService')

    const blobFile = await opensilex.getBlobFileFromPostOrGetService(
      securityService.getGdprFilePath(),
      'GET',
      language,
      null
    )

    if (encodedPdfUrl.value) {
      URL.revokeObjectURL(encodedPdfUrl.value)
    }

    encodedPdfUrl.value = URL.createObjectURL(blobFile)
  } catch (e) {
    documentIsAvailable.value = false
    opensilex.errorHandler(e)
  }
}

watch(currentLanguage, () => {
  loadPdf()
})

onMounted(() => {
  loadPdf()
})

onBeforeUnmount(() => {
  if (encodedPdfUrl.value) {
    URL.revokeObjectURL(encodedPdfUrl.value)
  }
})
</script>

<style scoped lang="scss">
#pdf-container {
  width: 100%;
  height: 80vh;
}
</style>

<i18n>
en:
  GDPR:
    title: "GDPR"
    title-description: "Privacy policy"
    page-title: "privacy policy"
    no-config-file: "There is no GDPR file configured for your instance"
    error-document: "Error while getting the document."

fr:
  GDPR:
    title: "RGPD"
    title-description: "Politique RGPD"
    page-title: "Politique RGPD"
    no-config-file: "Aucun fichier RGPD n'est configuré pour votre instance"
    error-document: "Erreur lors de l'obtention du document."
</i18n>