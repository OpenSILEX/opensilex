import AnnotationDetails from '../components/annotations/list/form/AnnotationDetails.vue';

export default {
  title: 'Components/AnnotationDetails',
  component: AnnotationDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AnnotationDetails },
  setup() { return { args }; },
  template: '<AnnotationDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
