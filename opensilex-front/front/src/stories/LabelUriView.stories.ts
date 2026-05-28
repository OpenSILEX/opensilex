import LabelUriView from '../components/common/views/LabelUriView.vue';

export default {
  title: 'Components/LabelUriView',
  component: LabelUriView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { LabelUriView },
  setup() { return { args }; },
  template: '<LabelUriView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
