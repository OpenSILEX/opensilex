<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-target"
      title="component.menu.scientificObjects"
      description="ScientificObjectList.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
        )
      "
    >
      <opensilex-CreateButton
        @click="soForm.createScientificObject()"
        label="ExperimentScientificObjects.create-scientific-object"
      ></opensilex-CreateButton>
      <opensilex-ScientificObjectForm
        ref="soForm"
        @onUpdate="redirectToDetail"
        @onCreate="redirectToDetail"
      ></opensilex-ScientificObjectForm>
      &nbsp;
      <opensilex-CreateButton
        @click="importForm.show()"
        label="OntologyCsvImporter.import"
      ></opensilex-CreateButton>
      <opensilex-ScientificObjectCSVImporter
        ref="importForm"
        @csvImported="refresh()"
      ></opensilex-ScientificObjectCSVImporter>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ScientificObjectList> </opensilex-ScientificObjectList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
@Component
export default class ScientificObjectView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("soForm") readonly soForm!: any;
  @Ref("importForm") readonly importForm!: any;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  redirectToDetail(http) {
    this.$router.push({
      path:
        "/scientific-objects/details/" +
        encodeURIComponent(http.response.result),
    });
  }
}
</script>

<style scoped lang="scss">
</style>