import FacilityListView from '../components/facilities/FacilityListView.vue';

export default {
  title: 'Components/FacilityListView',
  component: FacilityListView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityListView },
  setup() { return { args }; },
  template: '<FacilityListView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
