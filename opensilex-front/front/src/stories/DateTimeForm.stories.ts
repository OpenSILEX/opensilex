import DateTimeForm from '../components/common/forms/DateTimeForm.vue';

export default {
  title: 'Components/DateTimeForm',
  component: DateTimeForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DateTimeForm },
  setup() { return { args }; },
  template: '<DateTimeForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
