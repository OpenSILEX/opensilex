<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-Card icon="ik#ik-info" label="SystemView.details">
          <template v-slot:body>
            <h4>{{ $t("SystemView.info") }}</h4>
            <b-row>
              <b-col cols="4">
                <opensilex-StringView
                  label="SystemView.title"
                  :value="versionInfo.title"
                ></opensilex-StringView> 

                <opensilex-UriView
                  title="SystemView.version"
                  :uri="getVersion()"
                  :value="versionInfo.version"
                  target="_blank"
                ></opensilex-UriView>

                <opensilex-UriView
                  title="SystemView.api-docs"
                  :uri="versionInfo.api_docs.url"
                  target="_blank"
                ></opensilex-UriView>

                <opensilex-StringView
                  label="SystemView.git-commit"
                  :copyValue="versionInfo.git_commit.commit_id"
                  :value="versionInfo.git_commit.commit_message"
                  :allowCopy="true"
                  copyTextMessage="SystemView.git-commit-copy"
                  target="_blank"
                >
                </opensilex-StringView>
                <opensilex-StringView
                  label="SystemView.copyright"
                  value=" © 2021 INRAE – Tous droits réservés "
                ></opensilex-StringView> 
              </b-col>
              <b-col>
                <opensilex-TextView
                  label="SystemView.description"
                  :value="versionInfo.description"
                ></opensilex-TextView>

                <opensilex-UriView
                  title="SystemView.contact"
                  :uri="versionInfo.contact.email"
                  :value="versionInfo.contact.email"
                  :href="'mailto:' + versionInfo.contact.email" 
                ></opensilex-UriView>

                <opensilex-UriView
                  title="SystemView.project"
                  :uri="versionInfo.contact.homepage"
                  value="OpenSILEX homepage"
                  target="_blank"
                ></opensilex-UriView>

                <opensilex-LabelUriView
                  label="SystemView.license"
                  :uri="versionInfo.license.url"
                  :value="versionInfo.license.name"
                  target="_blank"
                  :allowCopy="false"
                ></opensilex-LabelUriView>
              </b-col>
            </b-row>

            <hr />
            <h4>{{ $t("SystemView.loaded-modules") }}</h4>
            <opensilex-TableView
              v-if="
                versionInfo.modules_version != undefined &&
                versionInfo.modules_version.length > 0
              "
              :items="versionInfo.modules_version"
              :fields="modulesFields"
              :showCount="false"
              :withPagination="false"
              :fixedPageSize="30"
            >
            </opensilex-TableView>
          </template>
        </opensilex-Card>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {   versionInfoDTO } from "opensilex-core/index"; 

@Component
export default class SystemView extends Vue {
  $opensilex: any;
  $store: any;  
  versionInfo: versionInfoDTO = {};

  modulesFields: any[] = [
    {
      key: "name",
      label: "SystemView.name",
      sortable: false,
    },
    {
      key: "version",
      label: "SystemView.version",
      sortable: false,
    },
  ];

  created() {
     this.versionInfo = this.$opensilex.versionInfo;
  }

  getVersion() { 
    if (this.versionInfo.version != undefined && this.versionInfo.version.includes("SNAPSHOT")) {
      return this.versionInfo.github_page + "/releases";
    }
    return this.versionInfo.github_page + "/releases/tag/" + this.versionInfo.version;
  }
}
</script>

<style scoped lang="scss">


</style>
<i18n>
en:
  SystemView:
    title: System
    details: System details
    info: Informations
    version: Version
    project: Project homepage 
    contact: Contact e-mail
    api-docs: Web API
    git-commit : Last commit Id
    git-commit-copy : Copy last commit Id
    description : Description
    title-description : Informations about information system
    license: Software license
    loaded-modules: Loaded modules
    copyright : Copyright
    name : Name
    
fr:
  SystemView:
    title: Système
    details: Détails du système
    info: Informations
    version: Version
    project: Page du projet
    contact: E-mail du contact
    api-docs: API Web
    git-commit: Dernier id commit
    git-commit-copy: Copier le dernier id commit
    description: Description
    title-description: Informations à propos du système d'information
    license: Licence logicielle
    loaded-modules: Modules chargés
    copyright : Copyright
    name: Nom
</i18n>
