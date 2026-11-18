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
      @onUpdate="onGroupUpdated"
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
        <GermplasmGroupContentList ref="groupContentList" :selectedUri="selected.uri" />
        <DocumentTabList :uri="selected.uri" :search="false" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
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
import DocumentTabList from '../documents/DocumentTabList.vue';

//#region Public
const selected = ref<GermplasmGroupGetDTO>();
const filtersCollapsed = ref(true);
//#endregion

//#region Private
const { t } = useI18n();
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const helpModal = useTemplateRef<InstanceType<typeof Modal>>('helpModal');
const groupForm = useTemplateRef<InstanceType<typeof GroupGermplasmForm>>('groupForm');
const groupList = useTemplateRef<InstanceType<typeof GermplasmGroupList>>('groupList');
const groupContentList = useTemplateRef<InstanceType<typeof GermplasmGroupContentList>>('groupContentList');

function refreshGroupList() {
  groupList.value?.refresh();
}

async function onGroupUpdated(uri: string) {
  refreshGroupList();
  if (!selected.value || !opensilex.compareUris(selected.value.uri, uri)) return;

  try {
    const response = await service.getGermplasmGroup(uri);
    if (selected.value && opensilex.compareUris(selected.value.uri, uri)) {
      selected.value = response.response.result;
      groupContentList.value?.refresh();
    }
  } catch (error) {
    opensilex.errorHandler(error);
  }
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
    opensilex.showSuccessToast(group.name + ' ' + t('component.common.success.delete-success-message').trim());
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
