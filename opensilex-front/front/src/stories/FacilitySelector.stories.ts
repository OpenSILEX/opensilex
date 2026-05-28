import FacilitySelector from '../components/facilities/FacilitySelector.vue';

export default {
  title: 'Components/FacilitySelector',
  component: FacilitySelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilitySelector },
  setup() { return { args }; },
  template: '<FacilitySelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
