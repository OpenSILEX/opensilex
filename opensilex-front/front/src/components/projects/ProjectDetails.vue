<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      title="component.project.project"
      :description="name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" :returnTo="{path: '/projects'}">
        <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/project/details/' + encodeURIComponent(uri)}"
        >{{ $t('component.project.details') }}</b-nav-item>
        
      <!--   <b-nav-item
          :active="isDocumentTab()"
          :to="{path: '/project/documents/' + encodeURIComponent(uri)}"
        >{{ $t('component.project.documents') }}</b-nav-item> -->
      </template>
    
    </opensilex-PageActions>
     <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectDescription v-if="isDetailsTab()" :uri="uri"></opensilex-ProjectDescription>
        <opensilex-ProjectDocuments v-else-if="isDocumentTab()" :uri="uri"></opensilex-ProjectDocuments>
      </template>
    </opensilex-PageContent>

  </div>
</template>

<script lang="ts">

import VueRouter from "vue-router";
import { Component} from "vue-property-decorator";
import Vue from "vue";
@Component
export default class ProjectDetails extends Vue {
 
  uri = null;
  name: string = "";

  created() {
    this.uri = this.$route.params.uri;
    let query: any = this.$route.query;
    if (query.name) {
      this.name = decodeURI(query.name);
    }
    
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/project/details/");
  }

  isDocumentTab() {
    return this.$route.path.startsWith("/project/documents/");
  }

}
</script>

<style  lang="scss">
</style>

<i18n>
en:
    ProjectDetails:
        title: Detailled project view
        advanced: Advanced informations
fr:
    ProjectDetails:
        title: Vue détaillée du projet
        advanced: Informations avancées
</i18n>
