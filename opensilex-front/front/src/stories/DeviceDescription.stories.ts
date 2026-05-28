import DeviceDescription from '../components/devices/DeviceDescription.vue';

export default {
  title: 'Components/DeviceDescription',
  component: DeviceDescription,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceDescription },
  setup() { return { args }; },
  template: '<DeviceDescription v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
