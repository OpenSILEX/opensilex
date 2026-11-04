<template>
  <div class="container-fluid">
    <CreateButton
      @click="groupForm.showCreateForm()"
      label="component.group.add"
      class="createButton"
    >
    </CreateButton>

    <PageContent>
      <template v-slot>
        <GroupList
          ref="groupList"
          @onEdit="showEditForm($event)"
        ></GroupList>
      </template>
    </PageContent>

    <GroupForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
      ref="groupForm"
      createTitle="component.group.add"
      editTitle="component.group.update"
      @onCreate="groupList.refresh()"
      @onUpdate="groupList.updateSelectedGroup()"
    ></GroupForm>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { SecurityService } from "opensilex-security/index";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import {computed, inject, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import GroupList from "@/components/groups/GroupList.vue";
import GroupForm from "@/components/groups/GroupForm.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const service =  opensilex.getService<SecurityService>('opensilex.SecurityService');

const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})

const groupForm = useTemplateRef<InstanceType<typeof GroupForm>>('groupForm')
const groupList = useTemplateRef<InstanceType<typeof GroupList>>('groupList')

function showEditForm(dto: any){
    let copydto = JSON.parse(JSON.stringify(dto));
    groupForm.value.showEditForm(copydto);
  }
</script>

<style scoped lang="scss">

.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}

</style>

