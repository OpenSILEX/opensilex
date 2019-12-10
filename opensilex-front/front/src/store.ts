import Vue from 'vue'
import Vuex, { Store } from 'vuex'
import { User } from '@/users/User'
import VueRouter, { Route } from 'vue-router';
import { FrontConfigDTO } from './lib';

Vue.use(Vuex)
Vue.use(VueRouter)

let expireTimeout: any = undefined;
let loaderCount: number = 0;
let frontConfig: FrontConfigDTO;

const computeMenuRoutes = (user: User) => {
  let routes: Array<Route> = [];

  if (frontConfig != undefined) {
    
  }

  return routes;
};
const createRouter = (user: User) => {
  let routes = computeMenuRoutes(user);

  return new VueRouter({
    mode: 'history',
    routes: routes
  })
}

const router: any = createRouter(User.ANONYMOUS());

const resetRouter = (user: User) => {
  const newRouter: any = createRouter(user);
  router.matcher = newRouter.matcher;
}


export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false,
    router: router
  },
  mutations: {
    login(state, user: User) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      expireTimeout = setTimeout(() => {
        let method: any = "logout";
        this.commit(method);
      }, user.getExpirationMs());

      state.user = user;
      resetRouter(state.user);
    },
    logout(state) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      state.user = User.logout();
      resetRouter(state.user);
    },
    setConfig(state, config: FrontConfigDTO) {
      frontConfig = config;
    },
    showLoader(state) {
      if (loaderCount == 0) {
        state.loaderVisible = true;
      }
      loaderCount++;
    },
    hideLoader(state) {
      loaderCount--;
      if (loaderCount == 0) {
        state.loaderVisible = false
      }
    }
  },
  actions: {
  },
  modules: {
  }
})

