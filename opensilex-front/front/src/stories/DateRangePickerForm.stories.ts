import DateRangePickerForm from '../components/common/forms/DateRangePickerForm.vue';

export default {
  title: 'Components/DateRangePickerForm',
  component: DateRangePickerForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DateRangePickerForm },
  setup() { return { args }; },
  template: '<DateRangePickerForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
