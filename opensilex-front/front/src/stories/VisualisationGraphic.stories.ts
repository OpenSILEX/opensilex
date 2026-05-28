import VisualisationGraphic from '../components/home/dashboard/VisualisationGraphic.vue';

export default {
  title: 'Components/VisualisationGraphic',
  component: VisualisationGraphic,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VisualisationGraphic },
  setup() { return { args }; },
  template: '<VisualisationGraphic v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
