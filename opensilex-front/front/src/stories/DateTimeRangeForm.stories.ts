import DateTimeRangeForm from '../components/common/forms/DateTimeRangeForm.vue';

export default {
  title: 'Components/DateTimeRangeForm',
  component: DateTimeRangeForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DateTimeRangeForm },
  setup() { return { args }; },
  template: '<DateTimeRangeForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
