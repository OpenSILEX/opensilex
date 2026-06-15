<template>
  <div class="container-fluid">
    <PageHeader
        icon="fa#seedling"
        :title="t('title')"
        :description="t('description')"
        class="detail-element-header"
    ></PageHeader>
    <PageActions :returnButton="false">
      <HelpButton
          @click="showHelpModal = true"
          :label="t('component.common.help-button')"
          class="helpButton"
      ></HelpButton>
      <n-menu
          v-model:value="activeMenuOption"
          :options="menuOptions"
          mode="horizontal"
      />
    </PageActions>
    <PageContent>
      <template v-slot>
        <GermplasmView v-if="activeMenuOption === MENU_KEY_GERMPLASM"></GermplasmView>
        <GermplasmGroup v-else-if="activeMenuOption === MENU_KEY_GROUP"></GermplasmGroup>
        <GermplasmCreate v-if="activeMenuOption === MENU_KEY_CREATION"></GermplasmCreate>
      </template>
    </PageContent>
  </div>
  <n-modal
      v-model:show="showHelpModal"
      preset="card"
      :style="{ width: '600px' }"
  >
    <GermplasmHelp></GermplasmHelp>
  </n-modal>
</template>

<script setup lang="ts">
import {computed, h, onMounted, ref} from "vue";
import {RouterLink, useRoute} from "vue-router";
import {useI18n} from "vue-i18n";
import PageHeader from "@/components/layout/PageHeader.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageContent from "@/components/layout/PageContent.vue";
import GermplasmView from "@/components/germplasm/list/GermplasmView.vue";
import {MenuOption, NMenu, NModal} from "naive-ui";
import GermplasmGroup from "@/components/germplasm/group/GermplasmGroup.vue";
import {useStore} from "vuex";
import GermplasmCreate from "@/components/germplasm/creation/GermplasmCreate.vue";
import GermplasmHelp from "@/components/germplasm/list/GermplasmHelp.vue";
import HelpButton from "@/components/common/buttons/HelpButton.vue";

const store = useStore();
const route = useRoute();
const {t} = useI18n();

const showHelpModal = ref(false);

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const MENU_KEY_GERMPLASM = "germplasm";
const MENU_KEY_GROUP = "group"
const MENU_KEY_CREATION = "creation";

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: () => h(RouterLink, {to: {path: "/germplasm"}}, () => t("germplasmMenu")),
    key: MENU_KEY_GERMPLASM,
  },
  {
    label: () => h(RouterLink, {to: {path: "/germplasm/create"}}, () => t("germplasmCreateMenu")),
    key: MENU_KEY_CREATION,
    disabled: !user.value.hasCredential(credentials.value.CREDENTIAL_GERMPLASM_MODIFICATION_ID)
  },
  {
    label: () => h(RouterLink, {to: {path: "/germplasm/group"}}, () => t("germplasmGroupMenu")),
    key: MENU_KEY_GROUP,
  },
]);
const activeMenuOption = ref<string | null>(null);

onMounted(() => {
  if (route.path.startsWith("/germplasm/group")) {
    activeMenuOption.value = MENU_KEY_GROUP;
  } else if (route.path.startsWith("/germplasm/create")) {
    activeMenuOption.value = MENU_KEY_CREATION;
  } else {
    activeMenuOption.value = MENU_KEY_GERMPLASM;
  }
})
</script>

<style scoped></style>

<i18n>
en:
  title: Germplasm
  description: Manage Genetic Resources Information
  germplasmMenu: "Germplasm"
  germplasmGroupMenu: "Germplasm Group"
  germplasmCreateMenu: "Add and modify germplasms"
fr:
  title: Germplasm
  description: Manage Genetic Resources Information
  germplasmMenu: "Ressources Génétiques "
  germplasmGroupMenu: "Groupe de Ressources Génétiques"
  germplasmCreateMenu: "Ajouter et modifier des ressources génétiques"
</i18n>