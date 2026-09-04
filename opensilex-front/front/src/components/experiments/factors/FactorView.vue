<template>
  <div class="container-fluid">
    <PageHeader
      icon="fa#sun"
      :title="factor.name"
      description="component.menu.experimentalDesign.factors"
      class="detail-element-header"
    ></PageHeader>

    <PageActions
      :tabs="[]"
      :returnButton="true"
    >
      <template v-slot>
        <router-link
          class="nav-link ml-3, tab"
          :class="{ active: isDetailsTab() }"
          :to="{
            path: '/' + encodeURIComponent(xpUri) + '/factor/details/' + encodeURIComponent(uri),
          }"
          >{{ $t('component.common.details-label') }}
        </router-link>
        <router-link
            class="nav-link ml-3, tab"
          :class="{ active: isDocumentTab() }"
          :to="{
            path: '/' + encodeURIComponent(xpUri) + '/factor/document/' + encodeURIComponent(uri),
          }"
          >{{ $t('component.common.details.document') }}
        </router-link>
        <router-link
            class="nav-link ml-3, tab"
          :class="{ active: isAnnotationTab() }"
          :to="{
            path:
              '/' + encodeURIComponent(xpUri) + '/factor/annotations/' + encodeURIComponent(uri),
          }"
          >{{ $t('component.annotation.list-title') }}
        </router-link>
        <AnnotationModalForm
          v-if="
            isAnnotationTab() && user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
          "
          ref="annotationModalForm"
          :target="uri"
          @onCreate="updateAnnotations"
          @onUpdate="updateAnnotations"
        ></AnnotationModalForm>
      </template>
    </PageActions>
    <PageContent>
      <template v-slot>
        <FactorDetails
          v-if="isDetailsTab()"
          @onUpdate="loadFactor(uri)"
          @onUpdateReferences="callUpdateFactorService"
          @onDelete="deleteFactor(uri)"
          @onReload="loadFactor(uri)"
          :factor="factor"
          :experiment="xpUri"
        ></FactorDetails>

        <AnnotationList
          v-else-if="isAnnotationTab()"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
          @onEdit="annotationModalForm.showEditForm($event)"
        ></AnnotationList>

        <DocumentTabList
          v-else-if="isDocumentTab()"
          :uri="uri"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
        ></DocumentTabList>
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import Vue, { computed, inject, nextTick, ref, useTemplateRef } from 'vue';
import { onMounted } from 'vue';
// @ts-ignore
import { FactorDetailsGetDTO, FactorUpdateDTO, FactorsService } from 'core/index';
import HttpResponse, { OpenSilexResponse } from '../../../lib/HttpResponse';
import AnnotationList from '../../annotations/list/AnnotationList.vue';
import PageHeader from '@/components/layout/PageHeader.vue';
import PageActions from '@/components/layout/PageActions.vue';
import PageContent from '@/components/layout/PageContent.vue';
import FactorDetails from '@/components/experiments/factors/FactorDetails.vue';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import DocumentTabList from '@/components/documents/DocumentTabList.vue';
import AnnotationModalForm from '@/components/annotations/list/form/AnnotationModalForm.vue';

const uri = ref<string>('');
const xpUri = ref<string>('');
const opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const store = useStore();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const service = opensilex.getService<FactorsService>('FactorsService');

const factor = ref<any>({
  uri: null,
  name: null,
  category: null,
  description: null,
  exact_match: [],
  close_match: [],
  broader_match: [],
  narrower_match: [],
  factor_levels: [],
});

const annotationList = useTemplateRef<InstanceType<typeof AnnotationList>>('annotationList');
const annotationModalForm =
  useTemplateRef<InstanceType<typeof AnnotationModalForm>>('annotationModalForm');

const user = computed(() => {
  return store.state.user;
});

const credentials = computed(() => {
  return store.state.credentials;
});

function callUpdateFactorService(form: FactorUpdateDTO, done) {
  console.debug('callUpdateFactorService');
  if (form instanceof Promise) {
    form.then((factor) => {
      service.updateFactor(form).then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug('Updated factor', uri);
        // this.$router.push({
        //   path: '/' + encodeURIComponent(this.xpUri) +  "/factor/details/" + encodeURIComponent(uri),
        // });
      });
    });
  } else {
    service.updateFactor(form).then((http: HttpResponse<OpenSilexResponse<any>>) => {
      let uri = http.response.result;
      console.debug('Updated factor', uri);
      // this.$router.push({
      //   path: '/' + encodeURIComponent(this.xpUri) +  "/factor/details/" + encodeURIComponent(uri),
      // });
    });
  }
}

function deleteFactor(uri: any) {
  console.debug('check Associated factor ' + uri);
  service
    .deleteFactor(uri)
    .then(() => {
      let message =
        t('component.factor.label') +
        ' ' +
        uri +
        ' ' +
        t('component.common.success.delete-success-message');
      opensilex.showSuccessToast(message);
      router.push({
        path: '/experiment/factors/' + encodeURIComponent(xpUri.value),
      });
    })
    .catch((error) => {
      if (error.status === 400) {
        opensilex.showErrorToast(t('component.factor.isAssociatedTo'));
      } else {
        opensilex.errorHandler(error);
      }
    });
}

onMounted(() => {
  uri.value = decodeURIComponent(route.params.uri as string);
  xpUri.value = decodeURIComponent(route.params.xpUri as string);
  loadFactor(uri.value);
});

function loadFactor(uri: string) {
  service
    .getFactorByURI(uri)
    .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
      factor.value = http.response.result;
    })
    .catch(opensilex.errorHandler);
}

function isDetailsTab() {
  return route.path.startsWith('/' + encodeURIComponent(xpUri.value) + '/factor/details/');
}

function isAnnotationTab() {
  return route.path.startsWith('/' + encodeURIComponent(xpUri.value) + '/factor/annotations/');
}

function isDocumentTab() {
  return route.path.startsWith('/' + encodeURIComponent(xpUri.value) + '/factor/document/');
}

function updateAnnotations() {
  nextTick(() => {
    annotationList.value?.refresh();
  });
}
</script>

<style scoped lang="scss"></style>

<i18n>
en:
    component:
        factor:
            returnButton: Return to the factor list
            alert-delete: This factor is linked to existing experiments and can't be deleted


fr:
    component:
        factor:
            returnButton: Retourner à la liste des facteurs
            alert-delete: Ce facteur est lié à des expérimentations et ne peut-être supprimé

</i18n>
