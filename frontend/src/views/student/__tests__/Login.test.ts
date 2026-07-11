import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import Login from '../Login.vue'

const { pushMock, successMock, warningMock, errorMock, loginMock, authStore } = vi.hoisted(() => {
  const loginMock = vi.fn()
  return {
    pushMock: vi.fn(),
    successMock: vi.fn(),
    warningMock: vi.fn(),
    errorMock: vi.fn(),
    loginMock,
    authStore: {
      login: loginMock,
      userInfo: null as null | { roleCode?: string; firstLogin?: boolean },
    },
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: successMock,
      warning: warningMock,
      error: errorMock,
    },
  }
})

vi.mock('@/stores/student/auth', () => ({
  useAuthStore: () => authStore,
}))

const ThemeToggleStub = {
  template: '<button type="button">theme</button>',
}

const FormStub = {
  template: '<form><slot /></form>',
}

const FormItemStub = {
  template: '<div><slot /></div>',
}

const InputStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <input
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
}

const CheckboxStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <label>
      <input
        type="checkbox"
        :checked="modelValue"
        @change="$emit('update:modelValue', $event.target.checked)"
      />
      <slot />
    </label>
  `,
}

const ButtonStub = {
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
  emits: ['click'],
}

const RouterLinkStub = {
  template: '<a><slot /></a>',
}

function mountLogin() {
  return mount(Login, {
    global: {
      stubs: {
        AppThemeToggle: ThemeToggleStub,
        'router-link': RouterLinkStub,
        'el-form': FormStub,
        'el-form-item': FormItemStub,
        'el-input': InputStub,
        'el-checkbox': CheckboxStub,
        'el-button': ButtonStub,
      },
      mocks: {
        User: {},
        Lock: {},
      },
    },
  })
}

describe('Login view', () => {
  beforeEach(() => {
    pushMock.mockReset()
    successMock.mockReset()
    warningMock.mockReset()
    errorMock.mockReset()
    loginMock.mockReset()
    authStore.userInfo = null
  })

  it('renders login copy and remember checkbox', () => {
    const wrapper = mountLogin()

    expect(wrapper.text()).toContain('账户登录')
    expect(wrapper.text()).toContain('记住登录状态')
  })

  it('submits credentials and redirects student users to /student/home', async () => {
    loginMock.mockImplementation(async () => {
      authStore.userInfo = { roleCode: 'ROLE_STUDENT', firstLogin: false }
    })

    const wrapper = mountLogin()
    ;(wrapper.vm as any).formRef = {
      validate: vi.fn().mockResolvedValue(true),
    }
    ;(wrapper.vm as any).form.username = '2022001'
    ;(wrapper.vm as any).form.password = '123456'
    ;(wrapper.vm as any).form.remember = true

    await (wrapper.vm as any).handleLogin()

    expect(loginMock).toHaveBeenCalledWith('2022001', '123456', true)
    expect(successMock).toHaveBeenCalledWith('登录成功')
    expect(pushMock).toHaveBeenCalledWith('/student/home')
  })

  it('redirects first-login users to change-password page', async () => {
    loginMock.mockImplementation(async () => {
      authStore.userInfo = { roleCode: 'ROLE_TEACHER', firstLogin: true }
    })

    const wrapper = mountLogin()
    ;(wrapper.vm as any).formRef = {
      validate: vi.fn().mockResolvedValue(true),
    }
    ;(wrapper.vm as any).form.username = 't001'
    ;(wrapper.vm as any).form.password = '123456'

    await (wrapper.vm as any).handleLogin()

    expect(warningMock).toHaveBeenCalledWith('首次登录，请先修改密码')
    expect(pushMock).toHaveBeenCalledWith('/change-password')
  })

  it('shows backend error message when login fails', async () => {
    loginMock.mockRejectedValue(new Error('账号或密码错误'))

    const wrapper = mountLogin()
    ;(wrapper.vm as any).formRef = {
      validate: vi.fn().mockResolvedValue(true),
    }
    ;(wrapper.vm as any).form.username = 'bad-user'
    ;(wrapper.vm as any).form.password = 'bad-pass'

    await (wrapper.vm as any).handleLogin()

    expect(errorMock).toHaveBeenCalledWith('账号或密码错误')
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('does not call login when validation fails', async () => {
    const wrapper = mountLogin()
    ;(wrapper.vm as any).formRef = {
      validate: vi.fn().mockRejectedValue(new Error('invalid')),
    }

    await (wrapper.vm as any).handleLogin()
    await nextTick()

    expect(loginMock).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()
  })
})
