import Dashboard from '../components/home/Dashboard.vue';

export default {
  title: 'Components/Dashboard',
  component: Dashboard,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Dashboard },
  setup() { return { args }; },
  template: '<Dashboard v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
