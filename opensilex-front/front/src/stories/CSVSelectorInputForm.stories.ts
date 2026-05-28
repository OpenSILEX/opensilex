import CSVSelectorInputForm from '../components/common/forms/CSVSelectorInputForm.vue';

export default {
  title: 'Components/CSVSelectorInputForm',
  component: CSVSelectorInputForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CSVSelectorInputForm },
  setup() { return { args }; },
  template: '<CSVSelectorInputForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
