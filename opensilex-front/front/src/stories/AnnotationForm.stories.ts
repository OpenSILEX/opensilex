import AnnotationForm from '../components/annotations/list/form/AnnotationForm.vue';

export default {
  title: 'Components/AnnotationForm',
  component: AnnotationForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AnnotationForm },
  setup() { return { args }; },
  template: '<AnnotationForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
