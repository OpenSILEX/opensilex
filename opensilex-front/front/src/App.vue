<template>

  <div id="page-wrapper" class="customized" v-bind:class="{ wrapper: !$route.meta.public, embed: embed }">
    <opensilex-ToastContainer ref="toastContainer" class="toast-container"></opensilex-ToastContainer>

      <div v-if="isPublicRoute">
        <section id="content-wrapper" class="page-wrap"  v-bind:class="{ 'hidden-menu': !menuVisible }" >
          <div id="main-content">
            <main class="main-content">
              <router-view />
            </main>

            <footer v-if="!embed">
              <component :is="footerComponent"></component>
            </footer>
          </div>
        </section>
      </div>  
          
                
      <div v-else>
          <component :is="headerComponent"
            v-if="user.isLoggedIn() && !disconnected && !embed"
            :searchBoxIsActive="uriSearchBoxVisible"
            @uriGlobalSearch="handleUriGlobalSearchPressed"
          ></component>


          <header v-if="!embed" v-bind:class="{ 'logged-out': !user.isLoggedIn() || disconnected }">
            <component class="header-login" v-bind:is="loginComponent"></component>
          </header>

          <!-- notification message  -->
          <div
            v-if="displayNotificationMessage && notificationMessageDisplayed"
            :class="{
              'notificationWithMenu' : store.state.menuVisible,
              [notificationColorClass]: true
            }"
            class="notificationMessageContainer notificationMessageDefaultColor"
          >
            <span class="notificationText" v-html="t(notificationMessageDisplayed)"></span>
            <span class="notificationButtonContainer">
              <opensilex-Button
                label="component.common.close"
                icon="bi-x"
                :small="true"
                class="closeNotificationButton"
                @click="closeNotification()"
              ></opensilex-Button>
            </span>
          </div>

          <!-- URI search box -->
          <div
            v-show="uriSearchBoxVisible"
            class="uri-search-box">
            <opensilex-GlobalUriSearchBox
              @hideUriSearch="handleHideUriSearch"
            ></opensilex-GlobalUriSearchBox>

          </div>

          <section 
            id="content-wrapper" 
            class="page-wrap"  
            v-bind:class="{ 'hidden-menu': !menuVisible }" 
            v-if="user.isLoggedIn() && !disconnected"
          >
          <div>
            <component id="menu-container" v-if="!embed" :is="menuComponent"></component>
          </div>

            <div id="main-content">
              <main class="main-content">
            <n-message-provider>
                <router-view />
            </n-message-provider>

              </main>
            </div>
          </section>
        </div>
 </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, inject, getCurrentInstance } from 'vue';
import { useRoute , useRouter} from 'vue-router';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import { Carousel, Dropdown} from "bootstrap";
import OpenSilexVuePlugin from "./models/OpenSilexVuePlugin";
import ToastContainer from './components/common/ToastContainer.vue';
import { NConfigProvider, useMessage, NMessageProvider } from 'naive-ui';

const toastContainer = ref();
// import 'bootstrap/dist/css/bootstrap.min.css';
// import 'bootstrap/dist/js/bootstrap.bundle.min.js';

// Props definitions
const props = defineProps({
  embed: Boolean,
  headerComponent: String,
  loginComponent: String,
  menuComponent: String,
  footerComponent: String
});

// Setup composition API variables
const route = useRoute();
const router = useRouter();
const store = useStore();
const { t, locale } = useI18n();

// const opensilex = getCurrentInstance()?.proxy?.$opensilex;
const opensilex = inject('opensilex');
// const bvToast = inject('bvToast');

// Data properties
const notificationMessage = ref(undefined);
const notificationMessageDisplayed = ref("");
const notificationEndDate = ref("");
const notificationColorTheme = ref("");
const displayNotificationMessage = ref(false);
const uriSearchBoxVisible = ref(false);

// Computed properties
const lang = computed(() => store.state.lang);
const disconnected = computed(() => store.state.disconnected);
const user = computed(() => store.state.user);
const isLoaderVisible = computed(() => store.state.loaderVisible);
const menuVisible = computed(() => store.state.menuVisible);
const isPublicRoute = computed(() => route.meta.public);

// const resolvedHeaderComponent = ref();
const headerComponent = computed(() => {
  return opensilex.getConfig().headerComponent
})

const footerComponent = computed(() => {
  return opensilex.getConfig().footerComponent
})

const menuComponent = computed(() => {
  return opensilex.getConfig().menuComponent
})

// Computed for notification color class
const notificationColorClass = computed(() => {
  const theme = notificationColorTheme.value.toLowerCase();

  if (theme === 'information') {
    return 'notificationMessageInfoColor';
  } else if (theme === 'warning') {
    return 'notificationMessageWarningColor';
  } else {
    return 'notificationMessageDefaultColor';
  }
});

// Methods
const closeNotification = () => {
  displayNotificationMessage.value = false;
};


// recupere event du clic sur le bouton dans le header
const handleUriGlobalSearchPressed = () => {
  toggleUriSearchBox();
};

const handleHideUriSearch = () => {
  toggleUriSearchBox(false);
};

const toggleUriSearchBox = (visible) => {
  uriSearchBoxVisible.value = visible !== undefined ? visible : !uriSearchBoxVisible.value;
};

// Lifecycle hooks
 

onMounted(async () => {
  
  await router.isReady();
  

  const config = opensilex.getConfig();

// Created lifecycle - initialize data
const currentDate = new Date();
const formattedCurrentDate = currentDate.toISOString().slice(0, 10);
notificationMessage.value = config.notificationMessage;
notificationEndDate.value = config.notificationEndDate;
notificationColorTheme.value = config.notificationColorTheme;

 if (toastContainer.value && opensilex) {
    opensilex.setToastManager(toastContainer.value);
  }

try {
  if (notificationEndDate.value) {
    new Date(notificationEndDate.value);
  }
  if (!notificationEndDate.value || notificationEndDate.value > formattedCurrentDate) {
    displayNotificationMessage.value = true;

    notificationMessageDisplayed.value = notificationMessage.value[locale.value];
  }
} catch {
  // opensilex.showErrorToast(t("component.header.bad-notification-end-date"));
}
});

// capter le changement de langue pour changer message
watch(() => locale.value, (newLocale) => {
  if (notificationMessage.value) {
    notificationMessageDisplayed.value = notificationMessage.value[newLocale];
  }
});

defineExpose({
  toastContainer
})

</script>

<style lang="scss">
@use "./styles/common.scss" as *;
@import "./../theme/opensilex/main.css";

.toast-container {
  z-index: 9999 !important;
}

.wrapper.embed .page-wrap .main-content {
  margin-top: 0px;
  margin-left: 0px;
  padding: 15px;
}

.uri-search-box {
  position: fixed;
  max-width: 500px;
  top: 70px;
  right: 8%;
  padding: 20px;
  background-color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  z-index: 1030;
}

.notificationMessageContainer{
  display: flex;
  position: relative;
  z-index: 1029; // header has an index of 1030, must be lower to allow the display of dropdown buttons above
  width: 100%;
  min-height: 35px;
  top: 65px;
  padding: 0 10px 0 10px;
  justify-content: center;
  align-items: center;
  font-family: sans-serif;
  font-size: small;
  font-style: oblique;
  font-weight: bold;
  color: darkslategray;
}

.notificationMessageDefaultColor {
  background: linear-gradient(45deg, #CFD8D5, #B1D8CB);
}
.notificationMessageInfoColor {
  background: linear-gradient(45deg, #c8e7eb, #91c8db);
}
.notificationMessageWarningColor {
    background: linear-gradient(45deg, #ebc8c8, #db9191);
}

.notificationWithMenu {
  left: 241px;
  max-width: calc(100% - 241px);
}

.notificationButtonContainer {
  position: inherit;
  right: 0;
}

.closeNotificationButton{
  border: none;
  padding: 1px 8px;
  margin: 4px;
  font-size: 1.5em !important;
  color:rgba(101, 101, 101, 0.5);
  font-weight: bolder;
  cursor: pointer;
  background: none;
}

.closeNotificationButton:hover{
  color : #00A28C;
  background: none;
}

.notificationText {
  padding-right: 25px;
  max-width: calc(100% - 50px);
}


.uri-search-box {
  position: fixed;
  max-width: 500px;
  top: 70px;
  right: 8%;
  padding: 20px;
  background-color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  z-index: 1030;
}
</style>
