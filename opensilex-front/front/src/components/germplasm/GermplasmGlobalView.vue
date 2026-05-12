<template>
  <div class="container-fluid">
    <PageHeader
        icon="fa#seedling"
        title="GermplasmView.title"
        description="GermplasmView.description"
        class="detail-element-header"
    ></PageHeader>
    <PageActions :returnButton="false">
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
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed, h, onMounted, ref} from "vue";
import {RouterLink, useRoute} from "vue-router";
import {useI18n} from "vue-i18n";
import PageHeader from "@/components/layout/PageHeader.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageContent from "@/components/layout/PageContent.vue";
import GermplasmView from "@/components/germplasm/list/GermplasmView.vue";
import {MenuOption, NMenu} from "naive-ui";
import GermplasmGroup from "@/components/germplasm/group/GermplasmGroup.vue";

const route = useRoute();
const {t} = useI18n();

const MENU_KEY_GERMPLASM = "germplasm";
const MENU_KEY_GROUP = "group"

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: () => h(RouterLink, {to: {path: "/germplasm"}}, () => t("germplasmMenu")),
    key: MENU_KEY_GERMPLASM,
  },
  {
    label: () => h(RouterLink, {to: {path: "/germplasm/group"}}, () => t("germplasmGroupMenu")),
    key: MENU_KEY_GROUP,
  },
]);
const activeMenuOption = ref<string | null>(null);

onMounted(() => {
  activeMenuOption.value = route.path.startsWith("/germplasm/group")
      ? MENU_KEY_GROUP
      : MENU_KEY_GERMPLASM;
})
</script>

<style scoped></style>

<i18n>
en:
  germplasmMenu: "Germplasm"
  germplasmGroupMenu: "Germplasm Group"
fr:
  germplasmMenu: "Ressources Génétiques "
  germplasmGroupMenu: "Groupe de Ressources Génétiques"
</i18n>