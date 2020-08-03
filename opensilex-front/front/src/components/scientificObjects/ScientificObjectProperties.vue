<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      title="component.menu.infrastructures"
      description="component.infrastructure.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          @click="propertyForm.showCreateForm()"
          label="ScientificObjectProperties.create-property"
        ></opensilex-CreateButton>
        <opensilex-ModalForm
          ref="propertyForm"
          component="opensilex-OntologyPropertyForm"
          createTitle="component.group.add"
          editTitle="component.group.update"
          modalSize="lg"
          icon="ik#ik-users"
        ></opensilex-ModalForm>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-OntologyPropertyTreeView :domain="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"></opensilex-OntologyPropertyTreeView>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectProperties extends Vue {
  $opensilex: any;
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  ScientificObjectProperties:
    title: Scientific objects properties

fr:
  ScientificObjectProperties:
    title: Propriétés des objets scientifiques

</i18n>
