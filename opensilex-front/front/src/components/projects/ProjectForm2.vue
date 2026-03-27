<template>
  <div class="v-step-project-2">
    <!-- Related projects -->
    <opensilex-ProjectSelector
      label="component.project.relatedProjects"
      :multiple="true"
      v-model:projects="formDto.related_projects"
    />

    <!-- Coordinators -->
    <opensilex-PersonSelector
      label="component.project.coordinators"
      :multiple="true"
      :helpMessage="t('ProjectForm.coordinators')"
      v-model:persons="formDto.coordinators"
    />

    <!-- Scientific contacts -->
    <opensilex-PersonSelector
      label="component.project.scientificContacts"
      :multiple="true"
      :helpMessage="t('ProjectForm.scientificContacts')"
      v-model:persons="formDto.scientific_contacts"
    />

    <!-- Administrative contacts -->
    <opensilex-PersonSelector
      label="component.project.administrativeContacts"
      :multiple="true"
      :helpMessage="t('ProjectForm.administrativeContacts')"
      v-model:persons="formDto.administrative_contacts"
    />

    <!-- Objective help -->
    <div class="divHelpMsg" v-if="showHelpMsg">
      <p>{{ t('ProjectForm.help-msg') }}</p>
    </div>

    <!-- Objective -->
    <opensilex-TextAreaForm
      v-model:value="formDto.objective"
      label="component.project.objective"
      :placeholder="t('component.project.form-objective-placeholder')"
    />

    <!-- Description -->
    <opensilex-TextAreaForm
      v-model:value="formDto.description"
      label="component.project.description"
      :placeholder="t('component.project.form-description-placeholder')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProjectCreationDTO } from 'opensilex-core/index'

const props = defineProps<{
  form: ProjectCreationDTO
}>()

const { t } = useI18n({ useScope: 'local' })

const formDto = computed<ProjectCreationDTO>({
  get: () => props.form,
  set: (v) => Object.assign(props.form, v)
})

const showHelpMsg = computed(() => {
  return formDto.value.objective != null || formDto.value.description != null
})

function reset() {
}

function validate(): boolean {
  // Si on veut des règles, ajouter ici et retourner false si erreur.
  return true
}

defineExpose({
  reset,
  validate
})
</script>

<style scoped lang="scss">
// .divHelpMsg {
//   display: flex;
//   background-color: rgba(0, 162, 140, 0.1);
//   border: 1px solid rgba(0, 162, 140, 0.5);
//   border-radius: 5px;
//   padding: 1rem;
//   margin: 0.5rem 0;
//   p {
//     margin: 0;
//     padding: 0;
//   }
// }
</style>

<i18n>
en:
  ProjectForm:
    scientificContacts: Scientists involved in the project. They must already be declared in the system. If unavailable in the predefined list, people can be added from the Users menu, before the new project creation.
    administrativeContacts: Administrative personnel linked to the project (e.g. human ressources people). They must already be declared in the system.
    coordinators: Project coordinators
    help-msg: Important! The objective and description will be visible to all users. Please specify confidential information directly in the experience section.
fr:
  ProjectForm:
    scientificContacts: Scientifiques impliqués dans le projet. Uniquement les personnes existant dans le système. Si elles ne sont pas disponibles dans la liste prédéfinie, les personnes peuvent être ajoutées à partir du menu Utilisateurs, avant la création du nouveau projet.
    administrativeContacts: Personnel administratif lié au projet (par exemple, les personnes chargées des ressources humaines). Uniquement les personnes existant dans le système.
    coordinators: Coordinateurs du projet
    help-msg: Attention! L'objectif et la description seront visibles par tous les utilisateurs. Veuillez spécifier les informations confidentielles directement dans la section expérience.
</i18n>
