import DeviceSelector from '../components/devices/DeviceSelector.vue';

export default {
  title: 'Components/DeviceSelector',
  component: DeviceSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceSelector },
  setup() { return { args }; },
  template: '<DeviceSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
