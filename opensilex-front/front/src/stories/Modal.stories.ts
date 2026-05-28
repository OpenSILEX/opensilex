import Modal from '../components/common/views/Modal.vue';

export default {
  title: 'Components/Modal',
  component: Modal,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Modal },
  setup() { return { args }; },
  template: '<Modal v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
