import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/reports'
      },
      {
        path: '/reports',
        name: 'Reports',
        component: () => import('../views/Reports.vue'),
        meta: { permission: 'menu:report:view' }
      },
      {
        path: '/reports/:id',
        name: 'ReportDetail',
        component: () => import('../views/ReportDetail.vue'),
        meta: { permission: 'menu:report:view' }
      },
      {
        path: '/query',
        name: 'Query',
        component: () => import('../views/Query.vue'),
        meta: { permission: 'menu:query:execute' }
      },
      {
        path: '/datasources',
        name: 'Datasources',
        component: () => import('../views/Datasources.vue'),
        meta: { permission: 'menu:datasource:view' }
      },
      {
        path: '/datasources/:id/dictionary',
        name: 'DataDictionary',
        component: () => import('../views/DataDictionary.vue'),
        meta: { permission: 'menu:datasource:view' }
      },
      {
        path: '/masks',
        name: 'Masks',
        component: () => import('../views/Masks.vue'),
        meta: { permission: 'menu:mask:edit' }
      },
      {
        path: '/groups',
        name: 'Groups',
        component: () => import('../views/Groups.vue'),
        meta: { permission: 'menu:group:edit' }
      },
      {
        path: '/users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { permission: 'menu:user:view' }
      },
      {
        path: '/roles',
        name: 'Roles',
        component: () => import('../views/Roles.vue'),
        meta: { permission: 'menu:role:view' }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth !== false && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    // 如果有token但未初始化，先初始化
    if (userStore.token && !userStore.initialized) {
      userStore.init().then(() => next()).catch(() => next())
    } else {
      next()
    }
  }
})

export default router
