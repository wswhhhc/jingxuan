import { defineComponent, h } from 'vue'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'

function modelValueFromProps(props: Record<string, unknown>) {
  if ('modelValue' in props) return props.modelValue
  if ('currentPage' in props) return props.currentPage
  if ('pageSize' in props) return props.pageSize
  if ('fileList' in props) return props.fileList
  return undefined
}

export const RouterLinkStub = defineComponent({
  name: 'RouterLinkStub',
  props: {
    to: { type: [String, Object], default: '' },
  },
  setup(props, { slots }) {
    return () =>
      h('a', { 'data-to': typeof props.to === 'string' ? props.to : JSON.stringify(props.to) }, slots.default?.())
  },
})

export const CommentThreadStub = defineComponent({
  name: 'CommentThreadStub',
  props: {
    comment: { type: Object, required: true },
  },
  setup(props) {
    return () => h('div', { class: 'comment-thread-stub' }, String((props.comment as any).content || ''))
  },
})

function createModelStub(tag = 'div', propName = 'modelValue', eventName = 'update:modelValue') {
  return defineComponent({
    name: `Stub${tag}`,
    inheritAttrs: false,
    props: {
      modelValue: { type: null, default: undefined },
      currentPage: { type: null, default: undefined },
      pageSize: { type: null, default: undefined },
      fileList: { type: null, default: undefined },
      label: { type: null, default: undefined },
      value: { type: null, default: undefined },
      disabled: { type: Boolean, default: false },
      title: { type: String, default: '' },
    },
    emits: [eventName, 'change', 'click', 'clear', 'remove', 'current-change', 'row-click', 'confirm'],
    setup(props, { attrs, slots, emit }) {
      return () =>
        h(
          tag,
          {
            ...attrs,
            'data-prop': String(propName),
            'data-value': JSON.stringify(modelValueFromProps(props as any) ?? ''),
            onClick: () => emit('click'),
          },
          slots.default?.(),
        )
    },
  })
}

export const elementStubs = {
  'el-form': defineComponent({
    name: 'ElFormStub',
    setup(_, { slots }) {
      return () => h('form', {}, slots.default?.())
    },
  }),
  'el-form-item': createModelStub('div'),
  'el-input': createModelStub('input'),
  'el-select': createModelStub('select'),
  'el-option': createModelStub('option'),
  'el-button': defineComponent({
    name: 'ElButtonStub',
    emits: ['click'],
    setup(_, { slots, emit, attrs }) {
      return () => h('button', { ...attrs, onClick: () => emit('click') }, slots.default?.())
    },
  }),
  'el-icon': defineComponent({
    name: 'ElIconStub',
    setup(_, { slots }) {
      return () => h('i', {}, slots.default?.())
    },
  }),
  'el-upload': createModelStub('div', 'fileList', 'update:fileList'),
  'el-image': createModelStub('img'),
  'el-tag': createModelStub('span'),
  'el-radio-group': createModelStub('div'),
  'el-radio-button': createModelStub('button'),
  'el-popconfirm': defineComponent({
    name: 'ElPopconfirmStub',
    emits: ['confirm'],
    setup(_, { slots, emit }) {
      return () => h('div', { onClick: () => emit('confirm') }, [slots.reference?.(), slots.default?.()])
    },
  }),
  'el-empty': defineComponent({
    name: 'ElEmptyStub',
    props: { description: { type: String, default: '' } },
    setup(props) {
      return () => h('div', { class: 'el-empty-stub' }, props.description)
    },
  }),
  'el-pagination': defineComponent({
    name: 'ElPaginationStub',
    props: {
      currentPage: { type: Number, default: 1 },
      pageSize: { type: Number, default: 10 },
      total: { type: Number, default: 0 },
    },
    emits: ['update:current-page', 'update:page-size', 'current-change', 'change'],
    setup(props, { emit }) {
      return () =>
        h('div', {
          class: 'el-pagination-stub',
          'data-page': props.currentPage,
          'data-total': props.total,
          onClick: () => {
            emit('update:current-page', props.currentPage + 1)
            emit('current-change', props.currentPage + 1)
            emit('change', props.currentPage + 1)
          },
        })
    },
  }),
  'el-drawer': createModelStub('aside'),
  'el-date-picker': createModelStub('div'),
  'el-table': defineComponent({
    name: 'ElTableStub',
    props: { data: { type: Array, default: () => [] } },
    emits: ['row-click'],
    setup(props, { slots, emit }) {
      return () =>
        h('div', { class: 'el-table-stub' }, [
          h(
            'div',
            { class: 'el-table-rows' },
            (props.data as any[]).map((row, index) =>
              h(
                'button',
                {
                  class: 'row-trigger',
                  onClick: () => emit('row-click', row),
                },
                `row-${index}`,
              ),
            ),
          ),
          slots.default?.(),
        ])
    },
  }),
  'el-table-column': defineComponent({
    name: 'ElTableColumnStub',
    setup(_, { slots }) {
      return () => h('div', {}, slots.default?.({ row: {}, $index: 0 }))
    },
  }),
  'el-checkbox': createModelStub('input'),
  'el-slider': createModelStub('input'),
  'el-avatar': createModelStub('span'),
  'el-alert': createModelStub('div'),
  'el-dialog': createModelStub('section'),
  'el-divider': createModelStub('hr'),
  'el-timeline': createModelStub('div'),
  'el-timeline-item': createModelStub('div'),
  'el-switch': createModelStub('input'),
}

export async function mountView(component: any, options: Record<string, unknown> = {}) {
  const wrapper = mount(component, {
    ...options,
    global: {
      stubs: {
        ...elementStubs,
        'router-link': RouterLinkStub,
        ...(options.global as any)?.stubs,
      },
      ...(options.global as any),
    },
  })
  await flushPromises()
  return wrapper as VueWrapper<any>
}
