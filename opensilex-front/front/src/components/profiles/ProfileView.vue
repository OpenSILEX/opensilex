<template>
  <div class="container-fluid">
    <CreateButton
        v-if="
      user.hasCredential(
        credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
        @click="profileForm.showCreateForm()"
        label="component.profile.add"
        class="createButton">
    </CreateButton>

    <PageContent>
      <template v-slot>
        <ProfileList
            ref="profileList"
            :credentialsGroups="credentialsGroups"
            :isClickable="false"
            @onEdit="showEditForm($event)"
        ></ProfileList>
      </template>
    </PageContent>

    <ProfileForm
        ref="profileForm"
        :createTitle="t('component.profile.add')"
        :editTitle="t('component.profile.update')"
        @onSuccess="profileList.refresh()"
    ></ProfileForm>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import PageContent from "@/components/layout/PageContent.vue";
import ProfileList from "@/components/profiles/ProfileList.vue";
import ProfileForm from "@/components/profiles/ProfileForm.vue";
import {CredentialsGroupDTO} from "opensilex-security/model/credentialsGroupDTO";
import {useI18n} from "vue-i18n";

const store = useStore();
const {t} = useI18n();

const profileForm = useTemplateRef<InstanceType<typeof ProfileForm>>('profileForm');
const profileList = useTemplateRef<InstanceType<typeof ProfileList>>('profileList');

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const credentialsGroups = ref<Array<CredentialsGroupDTO>>([])

//#region Event handlers and watchers
function showEditForm(dto) {
  let copydto = JSON.parse(JSON.stringify(dto));
  profileForm.value.showEditForm(copydto);
}

//#endregion
</script>

<style scoped lang="scss">
.createButton {
  margin-bottom: 10px;
  margin-top: -15px
}
</style>
