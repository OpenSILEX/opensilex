import CheckboxForm from '../components/common/forms/CheckboxForm.vue';

export default {
  title: 'Components/CheckboxForm',
  component: CheckboxForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CheckboxForm },
  setup() { return { args }; },
  template: '<CheckboxForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
