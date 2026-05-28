import FacilitiesView from '../components/facilities/FacilitiesView.vue';

export default {
  title: 'Components/FacilitiesView',
  component: FacilitiesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilitiesView },
  setup() { return { args }; },
  template: '<FacilitiesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
