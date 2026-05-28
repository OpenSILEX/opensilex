import TableAsyncView from '../components/common/views/TableAsyncView.vue';

export default {
  title: 'Components/TableAsyncView',
  component: TableAsyncView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TableAsyncView },
  setup() { return { args }; },
  template: '<TableAsyncView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
