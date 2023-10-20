<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-Card icon="ik#ik-info" label="GDPR.title-description">
          <template v-slot:body>
            <h3 v-if="!documentIsSetInTheConfig"> {{ $t("GDPR.no-config-file") }} </h3>
            <Iframe
                v-else-if="documentIsAvailable"
                id="pdf-container"
                :src="encodedPdfUrl"
            />
            <h3 v-else> {{ $t("GDPR.error-document") }} </h3>
          </template>
        </opensilex-Card>
      </template>
    </opensilex-PageContent>
  </div>
</template>


<script lang="ts">
import Vue from 'vue';
import Component from "vue-class-component";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {Watch} from "vue-property-decorator";

@Component
export default class GDPR extends Vue {
  $opensilex: OpenSilexVuePlugin;

  documentIsSetInTheConfig:boolean = true;
  documentIsAvailable: boolean = true;
  encodedPdfUrl: string = "";

  get currentLanguage(): string {
    return this.$opensilex.getLang()
  }

  @Watch("currentLanguage")
  onLanguageChangeRefreshPdf()
  {
    this.loadPdf()
  }

  created() {
    this.loadPdf()
  }

  private async loadPdf(){
    this.documentIsSetInTheConfig = this.$opensilex.getConfig().gdprFileIsConfigured
    if (!this.documentIsSetInTheConfig){ return }

    try {
      const language = { language : this.currentLanguage }
      const securityService = this.$opensilex.getService<SecurityService>("opensilex.SecurityService")
      const blobFile =  await this.$opensilex.getBlobFileFromPostOrGetService(securityService.getGdprFilePath(), "GET", language, null)

      this.encodedPdfUrl = URL.createObjectURL(blobFile);
    } catch (e) {
      this.documentIsAvailable = false;
      this.$opensilex.errorHandler(e)
    }
  }

}
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