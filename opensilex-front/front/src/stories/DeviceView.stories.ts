import DeviceView from '../components/devices/DeviceView.vue';

export default {
  title: 'Components/DeviceView',
  component: DeviceView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceView },
  setup() { return { args }; },
  template: '<DeviceView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
