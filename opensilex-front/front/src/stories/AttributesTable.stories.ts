import AttributesTable from '../components/common/forms/AttributesTable.vue';

export default {
  title: 'Components/AttributesTable',
  component: AttributesTable,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AttributesTable },
  setup() { return { args }; },
  template: '<AttributesTable v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
