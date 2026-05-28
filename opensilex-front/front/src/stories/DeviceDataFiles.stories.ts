import DeviceDataFiles from '../components/devices/DeviceDataFiles.vue';

export default {
  title: 'Components/DeviceDataFiles',
  component: DeviceDataFiles,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceDataFiles },
  setup() { return { args }; },
  template: '<DeviceDataFiles v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
