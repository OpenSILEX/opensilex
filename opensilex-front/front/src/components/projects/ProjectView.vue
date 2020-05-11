<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-folder"
      title="component.menu.projects"
      description="component.project.search-description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton 
        @click="projectForm.showCreateForm()" 
        label="component.project.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectList
        ref="projectList" 
        @onEdit="projectForm.showEditForm($event)"
        ></opensilex-ProjectList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      ref="projectForm"
      component="opensilex-ProjectForm"
      createTitle="component.project.add"
      editTitle="component.project.update"
      modalSize="lg"
      icon="ik#ik-folder"
      @onCreate="projectList.refresh()"
      @onUpdate="projectList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
@Component
export default class ProjectView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("projectForm") readonly projectForm!: any;
  @Ref("projectList") readonly projectList!: any;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }
}
</script>

<style scoped lang="scss">
</style>