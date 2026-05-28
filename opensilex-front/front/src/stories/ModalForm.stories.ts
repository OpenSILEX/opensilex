import ModalForm from '../components/common/forms/ModalForm.vue';

export default {
  title: 'Components/ModalForm',
  component: ModalForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ModalForm },
  setup() { return { args }; },
  template: '<ModalForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
