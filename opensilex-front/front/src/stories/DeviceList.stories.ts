import DeviceList from '../components/devices/DeviceList.vue';

export default {
  title: 'Components/DeviceList',
  component: DeviceList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceList },
  setup() { return { args }; },
  template: '<DeviceList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
