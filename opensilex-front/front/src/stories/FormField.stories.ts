import FormField from '../components/common/forms/FormField.vue';

export default {
  title: 'Components/FormField',
  component: FormField,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FormField },
  setup() { return { args }; },
  template: '<FormField v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
