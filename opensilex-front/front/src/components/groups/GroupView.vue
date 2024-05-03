<template>
  <div class="container-fluid">
    <opensilex-CreateButton
      @click="groupForm.showCreateForm()"
      label="component.group.add"
      class="createButton"
    >
    </opensilex-CreateButton>

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
      @onUpdate="groupList.updateSelectedGroup()"
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

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}

</style>

