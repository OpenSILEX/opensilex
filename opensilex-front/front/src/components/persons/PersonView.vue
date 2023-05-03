<template>
  <div class="container-fluid">
    <opensilex-CreateButton
      @click="personForm.showCreateForm()"
      label="PersonView.create"
      class="createButton">
    </opensilex-CreateButton>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-PersonList
          ref="personList"
          @onEdit="showEditForm($event)"
        ></opensilex-PersonList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
      ref="PersonForm"
      component="opensilex-PersonForm"
      createTitle="PersonView.create"
      editTitle="PersonView.update"
      icon="ik#ik-user"
      @onCreate="personList.refresh()"
      @onUpdate="personList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../models/Store";

@Component
export default class PersonView extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;

  @Ref("PersonForm") readonly personForm!: any;
  @Ref("personList") readonly personList!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  showEditForm(dto){
    let copydto = JSON.parse(JSON.stringify(dto));
    this.personForm.showEditForm(copydto);
  }
}
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}
</style>

<i18n>
en:
  PersonView:
    title: Persons
    description: Manage persons
    create: Add a person
    update: Update a person

fr:
  PersonView:
    title: Personnes
    description: Gérer les personnes
    create: Ajouter une personne
    update: Modifier une personne
</i18n>