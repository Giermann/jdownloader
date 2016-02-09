package org.jdownloader.settings.advanced;

import org.jdownloader.gui.translate._GUI;

public class RangeValidator extends Validator {

    private long min;
    private long steps = 1;

    public long getMin() {
        return min;
    }

    public long getSteps() {
        return steps;
    }

    public void setSteps(long steps) {
        this.steps = steps;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public String toString() {
        return _GUI.T.RangeValidator_toString_object_(min, max);
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    private long max;

    public RangeValidator(long min, long max) {
        this.min = min;
        this.max = max;
    }

}
