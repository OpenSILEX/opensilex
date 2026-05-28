import EntityOfInterestView from '../components/variables/views/EntityOfInterestView.vue';

export default {
  title: 'Components/EntityOfInterestView',
  component: EntityOfInterestView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EntityOfInterestView },
  setup() { return { args }; },
  template: '<EntityOfInterestView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
