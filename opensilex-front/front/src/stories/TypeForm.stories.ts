import TypeForm from '../components/common/forms/TypeForm.vue';

export default {
  title: 'Components/TypeForm',
  component: TypeForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TypeForm },
  setup() { return { args }; },
  template: '<TypeForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
