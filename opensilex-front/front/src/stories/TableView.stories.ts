import TableView from '../components/common/views/TableView.vue';

export default {
  title: 'Components/TableView',
  component: TableView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TableView },
  setup() { return { args }; },
  template: '<TableView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
