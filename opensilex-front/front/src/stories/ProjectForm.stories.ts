import ProjectForm from '../components/projects/ProjectForm.vue';

export default {
  title: 'Components/ProjectForm',
  component: ProjectForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectForm },
  setup() { return { args }; },
  template: '<ProjectForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
