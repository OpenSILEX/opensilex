<template>
  <div class="container-fluid">
    <CreateButton
      @click="personForm.showCreateForm()"
      :label="t('component.person.add')"
      class="createButton">
    </CreateButton>

    <PageContent>
      <template v-slot>
        <PersonList
          ref="personList"
          @onEdit="showEditForm($event)"
        ></PersonList>
      </template>
    </PageContent>

    <ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
      ref="PersonForm"
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
import { computed, ref } from "vue";
import {OpenSilexStore} from "@/models/Store";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import PersonList from "@/components/persons/PersonList.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import {useI18n} from "vue-i18n";

const store = useStore() as OpenSilexStore;
const { t } = useI18n();

const personForm = ref<any>();
const personList: any = ref(null);

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

function showEditForm(dto){
  let copydto = JSON.parse(JSON.stringify(dto));
  this.personForm.showEditForm(copydto);
}
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}
</style>