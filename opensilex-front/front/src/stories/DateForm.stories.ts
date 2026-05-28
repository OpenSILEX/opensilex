import DateForm from '../components/common/forms/DateForm.vue';

export default {
  title: 'Components/DateForm',
  component: DateForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DateForm },
  setup() { return { args }; },
  template: '<DateForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
