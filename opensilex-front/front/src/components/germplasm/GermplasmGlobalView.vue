<template>
  <div class="container-fluid">
    <PageHeader
     :hasIcon="true"
     icon="fa#seedling"
     title="component.germplasms.title"
     description="component.germplasms.description"
     class="detail-element-header">
    </PageHeader>
    <PageActions :return-button="false">
      <template #default>
        <ul class="nav nav-tabs">
          <li class="nav-item">
            <RouterLink
              class="nav-link tab"
              to="/germplasm"
              exact-active-class="active"
            >
              {{ t("component.germplasms.title") }}
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink
              class="nav-link tab"
              to="/germplasm/group"
              exact-active-class="active"
            >
              {{ t("component.germplasms.list.filter.germplasm-group") }}
            </RouterLink>
          </li>
        </ul>
      </template>
    </PageActions>
    <PageContent>
      <template v-slot>
        <GermplasmView v-if="!isGroupGermplasmTab" />
        <GermplasmGroup v-else />
      </template>
    </PageContent>
  </div>
</template>






<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import PageHeader from '@/components/layout/PageHeader.vue';
import PageActions from '@/components/layout/PageActions.vue';
import PageContent from '@/components/layout/PageContent.vue';
import GermplasmView from '@/components/germplasm/GermplasmView.vue';
import GermplasmGroup from '@/components/germplasmGroup/GermplasmGroup.vue';

const route = useRoute();
const { t } = useI18n();
const isGroupGermplasmTab = computed(() => route.path.startsWith('/germplasm/group'));
</script>

<style scoped lang="scss">
.tab {
  text-decoration: none;
  color: var(--color-dark);
}
</style>
