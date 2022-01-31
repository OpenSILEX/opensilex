<template>
  <div class="container-fluid">
    <!-- <opensilex-PageHeader
      icon="ik#ik-folder"
      title="component.menu.projects"
      description="component.project.search-description"
    ></opensilex-PageHeader> -->

    <opensilex-PageActions
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
    >
      <template v-slot>
        <opensilex-CreateButton @click="projectForm.showCreateForm()" label="component.project.add"></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectList ref="projectList" @onEdit="projectForm.showEditForm($event)"></opensilex-ProjectList>
      </template>
    </opensilex-PageContent>
    <opensilex-ProjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="projectForm"
      @onCreate="redirectToCreatedProject"
      @onUpdate="projectList.refresh()"
    ></opensilex-ProjectForm>
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

  redirectToCreatedProject(project) {
    this.$router.push({
      path: '/project/details/' + encodeURIComponent(project.uri)
    })
    
  }
}
</script>

<style scoped lang="scss">
</style>