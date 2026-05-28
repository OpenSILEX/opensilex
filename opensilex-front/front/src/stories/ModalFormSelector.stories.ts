import ModalFormSelector from '../components/variables/form/ModalFormSelector.vue';

export default {
  title: 'Components/ModalFormSelector',
  component: ModalFormSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ModalFormSelector },
  setup() { return { args }; },
  template: '<ModalFormSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
