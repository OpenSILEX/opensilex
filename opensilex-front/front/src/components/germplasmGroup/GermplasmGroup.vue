<template>
  <div class="container-fluid">
    <PageActions :return-button="false">
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

    <Modal ref="helpModal" modal-size="xl">
      <GermplasmGroupHelp />
    </Modal>
    <GermplasmGroupForm
      ref="groupForm"
      @on-create="refreshGroupList"
      @on-update="refreshGroupList"
    />
    <div class="row">
      <div :class="selected ? 'col-md-6' : 'col-12'">
        <GermplasmGroupList
          ref="groupList"
          @select="selected = $event"
          @edit="groupForm?.showEditForm($event)"
          @delete="onGroupDelete"
        />
      </div>

      <div v-if="selected" class="col-md-6">
        <GermplasmGroupStructureDetails :selected="selected" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue';
import { useStore } from 'vuex';
import { useDialog } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { GermplasmService } from 'opensilex-core/lib';
import type { GermplasmGroupGetDTO } from 'opensilex-core/index';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import PageActions from '@/components/layout/PageActions.vue';
import Modal from '@/components/common/views/Modal.vue';
import HelpButton from '@/components/common/buttons/HelpButton.vue';
import CreateButton from '@/components/common/buttons/CreateButton.vue';
import GermplasmGroupForm from '@/components/germplasmGroup/GermplasmGroupForm.vue';
import GermplasmGroupHelp from '@/components/germplasmGroup/GermplasmGroupHelp.vue';
import GermplasmGroupList from '@/components/germplasmGroup/GermplasmGroupList.vue';
import GermplasmGroupStructureDetails from '@/components/germplasmGroup/GermplasmGroupStructureDetails.vue';

//#region Public
const selected = ref<GermplasmGroupGetDTO>();
//#endregion

//#region Private
const store = useStore();
const { t } = useI18n();
const dialog = useDialog();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const helpModal = useTemplateRef<InstanceType<typeof Modal>>('helpModal');
const groupForm = useTemplateRef<InstanceType<typeof GermplasmGroupForm>>('groupForm');
const groupList = useTemplateRef<InstanceType<typeof GermplasmGroupList>>('groupList');

function refreshGroupList() {
  groupList.value?.refresh();
}

function onGroupDelete(group: GermplasmGroupGetDTO) {
  dialog.error({
    content: t('component.common.delete-confirmation').toString(),
    positiveText: t('component.common.delete').toString(),
    negativeText: t('component.common.cancel').toString(),
    showIcon: false,
    closable: false,
    onPositiveClick: async () => {
      try {
        await service.deleteGermplasmGroup(group.uri);
        selected.value = undefined;
        refreshGroupList();
      } catch (error) {
        opensilex.errorHandler(error);
      }
    },
  });
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
