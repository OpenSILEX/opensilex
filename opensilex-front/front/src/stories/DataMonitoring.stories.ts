import DataMonitoring from '../components/home/dashboard/DataMonitoring.vue';

export default {
  title: 'Components/DataMonitoring',
  component: DataMonitoring,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataMonitoring },
  setup() { return { args }; },
  template: '<DataMonitoring v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
