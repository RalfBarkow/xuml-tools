package xuml.tools.model.compiler.runtime.query;

import xuml.tools.model.compiler.runtime.Entity;

public abstract class StringExpression<T extends Entity<T>> {

	public BooleanExpression<T> eq(StringExpression<T> e) {
		return new StringComparison<T>(this, StringComparisonOperator.EQ, e);
	}

	public BooleanExpression<T> neq(StringExpression<T> e) {
		return new StringComparison<T>(this, StringComparisonOperator.NEQ, e);
	}
}
