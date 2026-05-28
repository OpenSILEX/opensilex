import FacilityView from '../components/facilities/FacilityView.vue';

export default {
  title: 'Components/FacilityView',
  component: FacilityView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityView },
  setup() { return { args }; },
  template: '<FacilityView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
