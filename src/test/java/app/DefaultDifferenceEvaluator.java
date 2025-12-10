package app;

import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

public class DefaultDifferenceEvaluator implements DifferenceEvaluator {
    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        return null;
    }
}
