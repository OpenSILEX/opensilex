import LocationList from '../components/location/list/LocationList.vue';

export default {
  title: 'Components/LocationList',
  component: LocationList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { LocationList },
  setup() { return { args }; },
  template: '<LocationList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
