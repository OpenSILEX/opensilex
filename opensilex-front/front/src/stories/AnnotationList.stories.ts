import AnnotationList from '../components/annotations/list/AnnotationList.vue';

export default {
  title: 'Components/AnnotationList',
  component: AnnotationList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AnnotationList },
  setup() { return { args }; },
  template: '<AnnotationList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
