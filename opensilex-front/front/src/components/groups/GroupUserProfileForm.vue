<template>
  <div>
    <AccountSelector
      v-model:accounts="searchedUser"
      label="component.group.user-profiles"
      :multiple="false"
      @selectionChange="onAccountSelectorSelectionChange"
    />
    <n-data-table
      size="small"
      striped
      :columns="selectedFields"
      :data="userProfiles"
      :row-key="(row: GroupUserProfileDTO) => row.user_uri"
      :pagination="pagination"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, h, inject, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NDataTable, NSelect } from 'naive-ui'
import type { DataTableColumns, PaginationProps, SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { SecurityService, ProfileGetDTO, GroupUserProfileDTO } from 'opensilex-security/index'
import AccountSelector from '@/components/account/AccountSelector.vue'
import Icon from '@/components/common/views/Icon.vue'

/** Account picked in the AccountSelector */
interface SelectedAccount {
  uri: string
  label: string
}

//#region Public
const userProfiles = defineModel<GroupUserProfileDTO[]>('profiles', { required: true })
//#endregion

//#region Private
//#region Plugins & services
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<SecurityService>('opensilex.SecurityService')
//#endregion

//#region Data & Computed
const searchedUser = ref<string | null>()

const profilesList = ref<ProfileGetDTO[]>([])

const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 5,
  onUpdatePage: (page: number) => {
    pagination.page = page
  }
})

const selectedFields = computed<DataTableColumns<GroupUserProfileDTO>>(() => [
  {
    key: 'selected',
    title: '',
    width: 40,
    render: (row) =>
      h(
        NButton,
        { quaternary: true, type: 'error', size: 'tiny', onClick: () => unselectUserProfile(row) },
        { icon: () => h(Icon, { icon: 'fa#trash-alt' }) }
      )
  },
  {
    key: 'firstName',
    title: t('component.common.name'),
    sorter: (a, b) => (a.user_name ?? '').localeCompare(b.user_name ?? ''),
    render: (row) => row.user_name
  },
  {
    key: 'admin',
    title: t('component.profile.label'),
    render: (row) =>
      h(NSelect, {
        size: 'small',
        class: 'profile-selector',
        options: profileOptionsWithFallback.value,
        value: normalizeUri(row.profile_uri),
        onUpdateValue: (newUri: string) => onProfileChange(row, newUri)
      })
  }
])

// Builds the list of profile options from the profiles returned by the API
const profileOptions = computed<SelectOption[]>(() =>
  (profilesList.value || []).map((profil) => ({
    value: profil.uri,
    label: profil.name
  }))
)

// Adds to the options the profiles already used in the group but absent from the API : getAllProfiles()
const profileOptionsWithFallback = computed<SelectOption[]>(() => {
  const existingOptions = profileOptions.value

  const knownProfileUris = new Set(existingOptions.map((option) => option.value))

  const missingOptions = (userProfiles.value || [])
    .map((userProfile) => ({
      value: userProfile.profile_uri,
      label: userProfile.profile_name || userProfile.profile_uri
    }))
    .filter((option) => option.value && !knownProfileUris.has(option.value))

  return [...missingOptions, ...existingOptions]
})
//#endregion

//#region Hooks & watchers
onMounted(async () => {
  clearForm()

  const http = await service.getAllProfiles()
  profilesList.value = http.response.result
})
//#endregion

//#region Event handlers
function onAccountSelectorSelectionChange(option: SelectOption | SelectOption[] | undefined) {
  // single selector: the option is never an array
  const selected = option as SelectOption | undefined
  if (!selected?.value) {
    return
  }
  selectUser({ uri: String(selected.value), label: String(selected.label) })
}
//#endregion

//#region Methods
function clearForm() {
  pagination.page = 1
  searchedUser.value = null
}

// Updates the selected profile for a user and synchronizes the URI and label
function onProfileChange(item: GroupUserProfileDTO, newUri: string) {
  item.profile_uri = newUri

  // If the profile is known, the name is also synchronized.
  const profilOptions = profileOptionsWithFallback.value.find((option) => option.value === newUri)
  if (profilOptions) {
    item.profile_name = String(profilOptions.label)
  } else {
    // Fallback
    item.profile_name = item.profile_name || newUri
  }
}

function normalizeUri(u: string | undefined) {
  return String(u ?? '').trim().replace(/\/+$/, '')
}

function selectUser(user: SelectedAccount) {
  const index = userProfiles.value.findIndex((up) => up.user_uri == user.uri)
  if (index > -1) {
    return
  }

  const user_profile: GroupUserProfileDTO = {
    user_uri: user.uri,
    user_name: user.label
  }

  if (profilesList.value.length > 0) {
    user_profile.profile_uri = profilesList.value[0].uri
  }
  userProfiles.value.push(user_profile)
}

function unselectUserProfile(userProfile: GroupUserProfileDTO) {
  const index = userProfiles.value.findIndex((up) => up.user_uri == userProfile.user_uri)
  if (index > -1) {
    userProfiles.value.splice(index, 1)
  }
}
//#endregion
//#endregion

defineExpose({ clearForm })
</script>

<style scoped lang="scss">
.profile-selector {
  min-width: 150px;
}
</style>
