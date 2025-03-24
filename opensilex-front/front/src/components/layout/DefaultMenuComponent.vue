<template>
  <div>
    <!-- Hamburger -->
    <div class="hamburger-container">
      <button
        class="hamburger hamburger--collapse"
        :class="{ 'is-active': menuVisible }"
        type="button"
        @click="toggleMenu"
      >
        <span class="hamburger-box">
          <span class="hamburger-inner"></span>
        </span>
      </button>
    </div>

    <!-- Sections menu -->
    <div class="app-sidebar">
      <div class="sidebar-content">
        <div class="nav-container">
          <nav id="main-menu-navigation" class="navigation-main">
            <div
              v-for="item in menu"
              :key="item.id"
              class="nav-item"
              :class="{
                'has-sub': item.hasChildren(),
                open: item.showChildren,
                active: isActive(item),
              }"
            >
              <a v-if="item.hasChildren()" href="#" @click.prevent="toggle(item)">
                <i class="ik" :class="getIcon(item)"></i>
                <span>{{ t(item.label) }}</span>
              </a>
              <router-link v-else :to="item.route.path">
                <i class="ik" :class="getIcon(item)"></i>
                <span>{{ t(item.label) }}</span>
              </router-link>
              <div class="submenu-content" :class="{ open: item.showChildren }">
                <span @click="toggleMenuOnSelect">
                  <router-link
                    v-for="itemChild in item.children"
                    :key="itemChild.id"
                    :class="{
                      'is-shown': item.showChildren,
                      active: isActive(itemChild),
                    }"
                    class="menu-item"
                    :to="itemChild.route.path"
                  >
                    {{ t(itemChild.label) }}
                  </router-link>
                </span>
              </div>
            </div>

            <div class="nav-item">
              <a
                v-if="versionInfo?.api_docs?.url"
                :href="versionInfo.api_docs.url"
                target="_blank"
                class="router-link-exact-active router-link-active"
              >
                <img
                  fluid
                  width="25"
                  src="https://cdn.svgporn.com/logos/swagger.svg"
                />
                &nbsp;&nbsp;&nbsp;
                <span class="ml-1">{{ t("component.menu.web-api") }}</span>
              </a>
            </div>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useStore } from "vuex"; // ou Pinia selon ton projet
import { useRoute } from "vue-router";
import { Menu } from "../../models/Menu";
import { versionInfoDTO } from "opensilex-core/index";
import { useI18n } from 'vue-i18n';

const { t } = useI18n(); 
const store = useStore();
const route = useRoute();

const menu = computed(() => store.state.menu);
const user = computed(() => store.state.user);
const menuVisible = computed(() => store.state.menuVisible);

const versionInfo = ref<versionInfoDTO | null>(null);

onMounted(() => {
  versionInfo.value = store.state.versionInfo;
});

const toggleMenu = () => {
  store.commit("toggleMenu");
};

const toggleMenuOnSelect = () => {
  if (document.body.clientWidth < 1040) {
    store.commit("toggleMenuOnSelect");
  }
};

const toggle = (item: Menu) => {
  if (item.hasChildren()) {
    item.showChildren = !item.showChildren;
  }
};

const getIcon = (item: Menu): string => {
  const code = "icon." + item.label;
  const result = t(code);
  return result !== code ? result.toString() : "ik-folder";
};

const isActive = (item: Menu): boolean => {
  return item.route && route.path.startsWith(item.route.path);
};
</script>

<style scoped lang="scss">
.hamburger-container {
  position: fixed;
  z-index: 1030;
  -webkit-user-select: none;
  user-select: none;
  height: 65px;
  width: 60px;
  left: 0px;
  top: 0px;
  background-color: #00a38d;
}

.hamburger {
  position: relative;
  height: 55px;
  top: 1px;
  outline: none;
}

.hamburger .hamburger-inner,
.hamburger .hamburger-inner::after,
.hamburger .hamburger-inner::before,
.hamburger.is-active .hamburger-inner,
.hamburger.is-active .hamburger-inner:after ,
.hamburger.is-active .hamburger-inner:before {
  background-color: #fff;
}

.hamburger:hover .hamburger-inner,
.hamburger:hover .hamburger-inner::after,
.hamburger:hover .hamburger-inner::before {
  background-color: rgb(221, 221, 221);
}

.hamburger-box {
  width: 35px;
}

.hamburger-inner,
.hamburger-inner:after,
.hamburger-inner:before {
  width: 41px;
}

@media (min-width: 250px) and (max-width: 1150px) {
  .hamburger-inner,
  .hamburger-inner:after,
  .hamburger-inner:before {
    width: 25px;
  }
}
</style>