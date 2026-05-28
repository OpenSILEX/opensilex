import FacilityDetails from '../components/facilities/views/FacilityDetails.vue';

export default {
  title: 'Components/FacilityDetails',
  component: FacilityDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityDetails },
  setup() { return { args }; },
  template: '<FacilityDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
