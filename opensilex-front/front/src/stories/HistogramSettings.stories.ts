import HistogramSettings from '../components/home/dashboard/HistogramSettings.vue';

export default {
  title: 'Components/HistogramSettings',
  component: HistogramSettings,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { HistogramSettings },
  setup() { return { args }; },
  template: '<HistogramSettings v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
