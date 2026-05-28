import UriView from '../components/common/views/UriView.vue';

export default {
  title: 'Components/UriView',
  component: UriView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { UriView },
  setup() { return { args }; },
  template: '<UriView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
