//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.steps;

import org.opensilex.aiimport.create.CreationTarget;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * The things an import declares, and what each has to wait for: the one table the blockers of the
 * creations, the API and the guided path of the screen all read.
 * <p>
 * An experiment comes before a factor, a factor before its levels, and the objects and the data
 * after what they name. The constants are in the order the path walks them, so that a step comes
 * after everything it depends on; a new step is an entry here, a translation, and the way it is
 * evaluated in {@link StepEvaluator}.
 *
 * @author Arnaud Charleroy
 */
public enum ImportStep {

    PROJECTS(CreationTarget.PROJECT, false),
    VARIABLES(CreationTarget.VARIABLE, false),

    /**
     * Created one by one, with the platform's own form: no bulk creation exists for them.
     */
    FACILITIES(null, false),

    /**
     * Its projects, organisations and facilities are proposed when they exist, none is required.
     */
    EXPERIMENT(CreationTarget.EXPERIMENT, false),

    /**
     * Declared for an experiment, one at a time: an experiment can have several.
     */
    FACTORS(CreationTarget.FACTORS, true, EXPERIMENT),

    /**
     * The levels of a factor, which the platform creates with it, and adds to it when it is edited.
     */
    FACTOR_LEVELS(CreationTarget.FACTORS, true, FACTORS),

    /**
     * Named in an experiment, with the levels their columns give them.
     */
    OBJECTS(CreationTarget.SCIENTIFIC_OBJECTS, true, EXPERIMENT, FACTOR_LEVELS),

    /**
     * Observations: they need an experiment, the variables they measure, and the objects they sit on.
     * The creation checks each of these row by row and says how many are missing, which this table
     * could not, so it does not add its own blockers.
     */
    DATA(CreationTarget.DATA, false, EXPERIMENT, VARIABLES, OBJECTS),

    /**
     * What happened to the objects or at the facilities, of a type the user chose: they need the
     * experiment and the objects they concern. The creation checks the targets itself, one by one.
     */
    EVENTS(CreationTarget.EVENT, false, EXPERIMENT, OBJECTS);

    public static final String LABEL_KEY_PREFIX = "AiImport.steps.step_";
    public static final String BLOCK_KEY_PREFIX = "AiImport.proposal.block.after.";

    private final CreationTarget target;
    private final boolean blocksCreation;
    private final ImportStep[] dependencies;

    ImportStep(CreationTarget target, boolean blocksCreation, ImportStep... dependencies) {
        this.target = target;
        this.blocksCreation = blocksCreation;
        this.dependencies = dependencies;
    }

    /**
     * @return the creation that declares this step, or empty when it has none of its own
     */
    public Optional<CreationTarget> target() {
        return Optional.ofNullable(target);
    }

    /**
     * @return whether the creation of this step is refused while a step it depends on is not done.
     * Not every creation: the data checks its prerequisites itself, with their counts.
     */
    public boolean blocksCreation() {
        return blocksCreation;
    }

    /**
     * @return the steps that have to be done before this one
     */
    public Set<ImportStep> dependsOn() {
        // Built here, not in the constructor: a set of this enum cannot be made while it is being made.
        Set<ImportStep> steps = EnumSet.noneOf(ImportStep.class);
        steps.addAll(Arrays.asList(dependencies));
        return steps;
    }

    public String labelKey() {
        return LABEL_KEY_PREFIX + name();
    }

    public String blockKey() {
        return BLOCK_KEY_PREFIX + name();
    }

    /**
     * @return the first step that declares what this creation writes
     */
    public static Optional<ImportStep> of(CreationTarget target) {
        return Arrays.stream(values()).filter(step -> step.target == target).findFirst();
    }
}
