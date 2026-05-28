import ModalListBuilder from '../components/common/views/ModalListBuilder.vue';

export default {
  title: 'Components/ModalListBuilder',
  component: ModalListBuilder,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ModalListBuilder },
  setup() { return { args }; },
  template: '<ModalListBuilder v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
