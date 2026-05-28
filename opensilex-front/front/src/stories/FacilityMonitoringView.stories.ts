import FacilityMonitoringView from '../components/facilities/views/FacilityMonitoringView.vue';

export default {
  title: 'Components/FacilityMonitoringView',
  component: FacilityMonitoringView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityMonitoringView },
  setup() { return { args }; },
  template: '<FacilityMonitoringView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
