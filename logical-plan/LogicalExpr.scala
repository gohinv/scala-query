package logical

import datatypes.Field

/** 
 * Logical expression for use in logical query plans. Logical expression
 * provides info needed during planning phase such as name / data type of expression.
 */
trait LogicalExpr:

    /**
      * Return metadata about the value that will be produced by this expression
      * when evaluated against a particular input.
      */
      def toField(input: LogicalPlan): Field