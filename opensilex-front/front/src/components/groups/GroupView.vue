<template>
  <div class="container-fluid">
    <!-- <opensilex-PageHeader
      icon="ik#ik-users"
      title="component.menu.security.groups"
      description="component.group.description"
    ></opensilex-PageHeader> -->

    <opensilex-PageActions v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)">
      <template v-slot>
        <opensilex-CreateButton @click="groupForm.showCreateForm()" label="component.group.add"></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-GroupList
          ref="groupList"
          @onEdit="showEditForm($event)"
        ></opensilex-GroupList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
      ref="groupForm"
      component="opensilex-GroupForm"
      createTitle="component.group.add"
      editTitle="component.group.update"
      modalSize="lg"
      icon="ik#ik-users"
      @onCreate="groupList.refresh()"
      @onUpdate="groupList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService } from "opensilex-security/index";

@Component
export default class GroupView extends Vue {
  $opensilex: any;
  $store: any;
  service: SecurityService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("groupForm") readonly groupForm!: any;
  @Ref("groupList") readonly groupList!: any;


  showEditForm(dto){
    let copydto = JSON.parse(JSON.stringify(dto));
    this.groupForm.showEditForm(copydto);
  }
}
</script>

<style scoped lang="scss">
</style>

