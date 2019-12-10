import Vue from 'vue'
import Vuex from 'vuex'
import { User } from '@/users/User'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS()
  },
  mutations: {
    login(state, user) {
      state.user = user;
    },
    logout(state) {
      state.user = User.logout();
    }
  },
  actions: {
  },
  modules: {
  }
})
