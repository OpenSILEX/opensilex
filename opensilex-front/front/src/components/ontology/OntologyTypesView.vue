<template>
  <div class="container-fluid">
    <opensilex-PageHeader v-if="isTypeTab()" :icon="icon" :title="typeTitle"></opensilex-PageHeader>
    <opensilex-PageHeader v-else :icon="icon" :title="propertiesTitle"></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="false">
      <template v-slot>
        <b-nav-item :active="isTypeTab()" :to="{path: typeURI}">{{ $t("OntologyTypesView.typeTitle") }}</b-nav-item>
        <b-nav-item :active="isPropertiesType()" :to="{path: propertiesURI}">{{ $t("OntologyTypesView.propertiesTitle") }}</b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-OntologyClassView
          v-if="isTypeTab()"
          :rdfType="rdfType"
          :icon="icon"
          :title="typeTitle"
        ></opensilex-OntologyClassView>
        <opensilex-OntologyPropertyView
          v-else
          :rdfType="rdfType"
          :icon="icon"
          :title="propertiesTitle"
        />
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class OntologyTypesView extends Vue {
  $opensilex: any;
  $route: any;

  @Prop()
  rdfType;

  @Prop()
  icon;

  @Prop()
  typeTitle;

  @Prop()
  typeURI;

  @Prop()
  propertiesTitle;

  @Prop()
  propertiesURI;

  isTypeTab() {
    return this.$route.path.startsWith(this.typeURI) && !this.isPropertiesType();
  }

  isPropertiesType() {
    return this.$route.path.startsWith(this.propertiesURI);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  OntologyTypesView:
    typeTitle: Types
    propertiesTitle: Properties

fr:
  OntologyTypesView:
    typeTitle: Types
    propertiesTitle: Propriétés
</i18n>
