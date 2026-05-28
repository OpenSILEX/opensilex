import IconView from '../components/common/views/IconView.vue';

export default {
  title: 'Components/IconView',
  component: IconView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { IconView },
  setup() { return { args }; },
  template: '<IconView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
