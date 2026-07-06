<template>
  <div class="container-fluid">
    <CreateButton
      @click="onCreateButtonClick"
      :label="t('component.person.add')"
      class="createButton">
    </CreateButton>

    <PageContent>
      <template v-slot>
        <PersonList
          ref="personList"
          @onEdit="onPersonListEdit($event)"
        ></PersonList>
      </template>
    </PageContent>

    <ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
      ref="personForm"
      component="opensilex-PersonForm"
      createTitle="component.person.add"
      editTitle="component.person.update"
      icon="ik#ik-user"
      @onCreate="personList.refresh()"
      @onUpdate="personList.refresh()"
    ></ModalForm>
  </div>
</template>

<script setup lang="ts">
import { useStore } from "vuex";
import {computed, Ref, ref, useTemplateRef} from "vue";
import {OpenSilexStore} from "@/models/Store";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import PersonList from "@/components/persons/PersonList.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import {useI18n} from "vue-i18n";

const store = useStore() as OpenSilexStore;
const { t } = useI18n();

const personForm = useTemplateRef<InstanceType<typeof ModalForm>>('personForm');
const personList = useTemplateRef<InstanceType<typeof PersonList>>('personList');

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

//#region Event handlers and watchers
function onCreateButtonClick(){
  personForm.value.showCreateForm()
}

function onPersonListEdit(dto){
  let copydto = JSON.parse(JSON.stringify(dto));
  personForm.value.showEditForm(copydto);
}

//#endregion
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}
</style>