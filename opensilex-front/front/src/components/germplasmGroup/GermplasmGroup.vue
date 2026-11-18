<template>
  <div class="container-fluid">
    <PageActions :returnButton="false">
      <HelpButton
        class="helpButton"
        label="component.common.help-button"
        :small="true"
        @click="helpModal?.show()"
      />
      <CreateButton
        v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
        class="createButton"
        label="component.germplasms.group.add"
        @click="groupForm?.showCreateForm()"
      />
    </PageActions>

    <Modal ref="helpModal" modalSize="xl">
      <GermplasmGroupHelp />
    </Modal>
    <GroupGermplasmForm
      ref="groupForm"
      @onCreate="refreshGroupList"
      @onUpdate="refreshGroupList"
    />
    <div class="row">
      <div :class="filtersCollapsed && selected ? 'col-md-6' : 'col-12'">
        <GermplasmGroupList
          ref="groupList"
          v-model:filtersCollapsed="filtersCollapsed"
          @select="selected = $event"
          @edit="onGroupEdit"
          @delete="onGroupDelete"
        />
      </div>

      <div v-if="filtersCollapsed && selected" class="col-md-6">
        <GermplasmGroupStructureDetails :selected="selected" />
        <GermplasmGroupContentList :selectedUri="selected.uri" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue';
import { useStore } from 'vuex';
import { GermplasmService } from 'opensilex-core/lib';
import type {
  GermplasmGroupGetDTO,
  GermplasmGroupUpdateDTO,
} from 'opensilex-core/index';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import PageActions from '@/components/layout/PageActions.vue';
import Modal from '@/components/common/views/Modal.vue';
import HelpButton from '@/components/common/buttons/HelpButton.vue';
import CreateButton from '@/components/common/buttons/CreateButton.vue';
import GroupGermplasmForm from '@/components/germplasmGroup/GroupGermplasmForm.vue';
import GermplasmGroupHelp from '@/components/germplasmGroup/GermplasmGroupHelp.vue';
import GermplasmGroupList from '@/components/germplasmGroup/GermplasmGroupList.vue';
import GermplasmGroupStructureDetails from '@/components/germplasmGroup/GermplasmGroupStructureDetails.vue';
import GermplasmGroupContentList from '@/components/germplasmGroup/GermplasmGroupContentList.vue';

//#region Public
const selected = ref<GermplasmGroupGetDTO>();
//#endregion

//#region Private
const filtersCollapsed = ref(true);
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const helpModal = useTemplateRef<InstanceType<typeof Modal>>('helpModal');
const groupForm = useTemplateRef<InstanceType<typeof GroupGermplasmForm>>('groupForm');
const groupList = useTemplateRef<InstanceType<typeof GermplasmGroupList>>('groupList');

function refreshGroupList() {
  groupList.value?.refresh();
}

async function onGroupEdit(group: GermplasmGroupGetDTO) {
  try {
    const groupResponse = await service.getGermplasmGroupWithGermplasms(group.uri);
    const details = groupResponse.response.result;
    const germplasms = (details.germplasm_list ?? []).map((germplasm) => ({
      ...germplasm,
      uri: opensilex.getLongUri(germplasm.uri),
    }));
    const form: GermplasmGroupUpdateDTO = {
      uri: details.uri ?? group.uri,
      name: details.name,
      description: details.description,
      germplasm_list: germplasms.map((germplasm) => germplasm.uri),
    };
    groupForm.value?.showEditForm(form, germplasms);
  } catch (error) {
    opensilex.errorHandler(error);
  }
}

async function onGroupDelete(group: GermplasmGroupGetDTO) {
  try {
    await service.deleteGermplasmGroup(group.uri);
    selected.value = undefined;
    refreshGroupList();
  } catch (error) {
    opensilex.errorHandler(error);
  }
}
//#endregion
</script>



<style scoped lang="scss">
.createButton,
.helpButton {
  margin-bottom: 10px;
  margin-right: 5px;
  margin-top: 5px;
}

.helpButton:hover {
  height: 36px !important;
}
</style>
