import TypeView from '../components/common/views/TypeView.vue';

export default {
  title: 'Components/TypeView',
  component: TypeView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TypeView },
  setup() { return { args }; },
  template: '<TypeView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
