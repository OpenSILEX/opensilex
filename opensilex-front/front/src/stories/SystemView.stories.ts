import SystemView from '../components/tools/SystemView.vue';

export default {
  title: 'Components/SystemView',
  component: SystemView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SystemView },
  setup() { return { args }; },
  template: '<SystemView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
