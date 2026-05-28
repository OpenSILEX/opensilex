import DataTable from '../components/home/DataTable.vue';

export default {
  title: 'Components/DataTable',
  component: DataTable,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataTable },
  setup() { return { args }; },
  template: '<DataTable v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
