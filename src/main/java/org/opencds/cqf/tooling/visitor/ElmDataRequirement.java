package org.opencds.cqf.tooling.visitor;

import org.hl7.cql.model.DataType;
import org.hl7.elm.r1.*;

import java.util.HashSet;
import java.util.Set;

public class ElmDataRequirement extends ElmExpressionRequirement {
    public ElmDataRequirement(VersionedIdentifier libraryIdentifier, Retrieve element) {
        super(libraryIdentifier, element);
    }

    public ElmDataRequirement(VersionedIdentifier libraryIdentifier, Retrieve element, Retrieve inferredFrom) {
        super(libraryIdentifier, element);
        this.inferredFrom = inferredFrom;
    }

    public Retrieve getRetrieve() {
        return (Retrieve)element;
    }

    public Retrieve getElement() {
        return getRetrieve();
    }

    private Retrieve inferredFrom;
    public Retrieve getInferredFrom() {
        return inferredFrom;
    }

    private AliasedQuerySource querySource;
    public AliasedQuerySource getQuerySource() {
        return querySource;
    }
    public void setQuerySource(AliasedQuerySource querySource) {
        this.querySource = querySource;
    }

    private static ElmDataRequirement inferFrom(ElmDataRequirement requirement) {
        Retrieve inferredRetrieve = ElmCloner.clone(requirement.getRetrieve());
        ElmDataRequirement result = new ElmDataRequirement(requirement.libraryIdentifier, inferredRetrieve, requirement.getRetrieve());
        result.propertySet = requirement.propertySet;
        return result;
    }

    private static ElmDataRequirement inferFrom(ElmQueryRequirement requirement) {
        ElmDataRequirement singleSourceRequirement = null;
        for (ElmDataRequirement dataRequirement : requirement.getDataRequirements()) {
            if (singleSourceRequirement == null) {
                singleSourceRequirement = dataRequirement;
            }
            else {
                singleSourceRequirement = null;
                break;
            }
        }
        if (singleSourceRequirement != null) {
            return inferFrom(singleSourceRequirement);
        }

        return new ElmDataRequirement(requirement.libraryIdentifier, getRetrieve(requirement.getQuery()));
    }

    public static ElmDataRequirement inferFrom(ElmExpressionRequirement requirement) {
        if (requirement instanceof ElmDataRequirement) {
            return inferFrom((ElmDataRequirement)requirement);
        }
        if (requirement instanceof ElmQueryRequirement) {
            return inferFrom((ElmQueryRequirement)requirement);
        }
        return new ElmDataRequirement(requirement.libraryIdentifier, getRetrieve(requirement.getExpression()));
    }

    private static Retrieve getRetrieve(Expression expression) {
        return (Retrieve)new Retrieve()
                .withLocalId(expression.getLocalId())
                .withLocator(expression.getLocator())
                .withResultTypeName(expression.getResultTypeName())
                .withResultTypeSpecifier(expression.getResultTypeSpecifier())
                .withResultType(expression.getResultType());
    }

    private Set<Property> propertySet;

    public void reportProperty(ElmPropertyRequirement propertyRequirement) {
        if (propertySet == null) {
            propertySet = new HashSet<Property>();
        }
        propertySet.add(propertyRequirement.getProperty());
    }

    private ElmConjunctiveRequirement conjunctiveRequirement;
    public ElmConjunctiveRequirement getConjunctiveRequirement() {
        ensureConjunctiveRequirement();
        return conjunctiveRequirement;
    }

    private void ensureConjunctiveRequirement() {
        if (conjunctiveRequirement == null) {
            conjunctiveRequirement = new ElmConjunctiveRequirement(libraryIdentifier, new Null());
        }
    }

    public void addConditionRequirement(ElmConditionRequirement conditionRequirement) {
        ensureConjunctiveRequirement();
        conjunctiveRequirement.combine(conditionRequirement);
    }

    // TODO:
    private void extractStatedRequirements(Retrieve retrieve) {
        if (retrieve.getIdProperty() != null || retrieve.getIdSearch() != null) {
            // Add as an OtherFilterElement
        }

        if (retrieve.getCodeProperty() != null || retrieve.getCodeSearch() != null) {
            // Build the left-hand as a Property (or Search) against the alias
            // The right-hand is the retrieve codes
            // Code comparator values are in, =, and ~ (may need to support ~in at some point...)
        }

        if (retrieve.getDateProperty() != null || retrieve.getDateSearch() != null || retrieve.getDateLowProperty() != null || retrieve.getDateHighProperty() != null) {
            // Build the left-hand as a Property (or Search) against the alias
            // The right-hand is the date range
            // The comparator is always during (i.e. X >= start and X <= end)
        }
    }

    private void applyConditionRequirementTo(ElmConditionRequirement conditionRequirement, Retrieve retrieve, ElmRequirementsContext context) {
        // if the column is terminology-valued, express as a code filter
        // if the column is date-valued, express as a date filter
        // else express as an other filter
        Property property = conditionRequirement.getProperty().getProperty();
        //DataType propertyType = property.getResultType();
        // Use the comparison type due to the likelihood of conversion operators along the property
        DataType comparisonType = conditionRequirement.getComparand().getExpression().getResultType();
        if (comparisonType != null) {
            if (context.getTypeResolver().isTerminologyType(comparisonType)) {
                CodeFilterElement codeFilter = new CodeFilterElement();
                if (property instanceof Search) {
                    codeFilter.setSearch(property.getPath());
                }
                else {
                    codeFilter.setProperty(property.getPath());
                }
                switch (conditionRequirement.getElement().getClass().getSimpleName()) {
                    case "Equal":
                        codeFilter.setComparator("=");
                        break;
                    case "Equivalent":
                        codeFilter.setComparator("~");
                        break;
                    case "In":
                    case "InValueSet":
                    case "AnyInValueSet":
                        codeFilter.setComparator("in");
                        break;
                }
                if (codeFilter.getComparator() != null) {
                    codeFilter.setValue(conditionRequirement.getComparand().getExpression());
                    retrieve.getCodeFilter().add(codeFilter);
                }
            }
            else if (context.getTypeResolver().isDateType(comparisonType)) {
                DateFilterElement dateFilter = new DateFilterElement();
                if (property instanceof Search) {
                    dateFilter.setSearch(property.getPath());
                }
                else {
                    dateFilter.setProperty(property.getPath());
                }
                // Determine operation and appropriate range
                // If right is interval-valued
                // If the operation is equal, equivalent, sameas, in, or included in, the date range is the comparand
                Expression comparand = conditionRequirement.getComparand().getExpression();
                if (context.getTypeResolver().isIntervalType(comparisonType)) {
                    switch (conditionRequirement.getElement().getClass().getSimpleName()) {
                        case "Equal":
                        case "Equivalent":
                        case "SameAs":
                        case "In":
                        case "IncludedIn":
                            dateFilter.setValue(comparand);
                            break;
                        case "Before":
                            dateFilter.setValue(new Interval().withLowClosed(true).withHigh(new Start().withOperand(comparand)).withHighClosed(false));
                            break;
                        case "SameOrBefore":
                            dateFilter.setValue(new Interval().withLowClosed(true).withHigh(new Start().withOperand(comparand)).withHighClosed(true));
                            break;
                        case "After":
                            dateFilter.setValue(new Interval().withLow(new End().withOperand(comparand)).withLowClosed(false).withHighClosed(true));
                            break;
                        case "SameOrAfter":
                            dateFilter.setValue(new Interval().withLow(new End().withOperand(comparand)).withLowClosed(true).withHighClosed(true));
                            break;
                        case "Includes":
                        case "Meets":
                        case "MeetsBefore":
                        case "MeetsAfter":
                        case "Overlaps":
                        case "OverlapsBefore":
                        case "OverlapsAfter":
                        case "Starts":
                        case "Ends":
                            // TODO: Might be better to turn these into date-based conjunctive requirements as part of condition requirement inference
                            break;
                    }
                }
                else {
                    switch (conditionRequirement.getElement().getClass().getSimpleName()) {
                        case "Equal":
                        case "Equivalent":
                        case "SameAs":
                            dateFilter.setValue(new Interval().withLow(comparand).withLowClosed(true).withHigh(comparand).withHighClosed(true));
                            break;
                        case "Less":
                        case "Before":
                            dateFilter.setValue(new Interval().withLowClosed(true).withHigh(comparand).withHighClosed(false));
                            break;
                        case "LessOrEqual":
                        case "SameOrBefore":
                            dateFilter.setValue(new Interval().withLowClosed(true).withHigh(comparand).withHighClosed(true));
                            break;
                        case "Greater":
                        case "After":
                            dateFilter.setValue(new Interval().withLow(comparand).withLowClosed(false).withHighClosed(true));
                            break;
                        case "GreaterOrEqual":
                        case "SameOrAfter":
                            dateFilter.setValue(new Interval().withLow(comparand).withLowClosed(true).withHighClosed(true));
                            break;
                    }
                }

                if (dateFilter.getValue() != null) {
                    retrieve.getDateFilter().add(dateFilter);
                }
            }
            else {

            }
        }
    }

    private void applyTo(Retrieve retrieve, ElmRequirementsContext context) {
        // for each ConditionRequirement
        // apply to the retrieve
        for (ElmExpressionRequirement conditionRequirement : getConjunctiveRequirement().getArguments()) {
            if (conditionRequirement instanceof ElmConditionRequirement) {
                applyConditionRequirementTo((ElmConditionRequirement)conditionRequirement, retrieve, context);
            }
        }
    }

    public void applyDataRequirements(ElmRequirementsContext context) {
        // If the source of the alias is a direct retrieve, query requirements can be applied directly
        // Otherwise, the query requirements are applied to an "inferred" retrieve representing the query source
        extractStatedRequirements(getRetrieve());
        applyTo(getRetrieve(), context);
    }
}
