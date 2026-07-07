package logical

import datatypes.Schema

class Projection(val input: LogicalPlan, val expr: List[LogicalExpr]) extends LogicalPlan:
    override def schema(): Schema =
        Schema(expr.map(_.toField(input)))
    
    override def children(): List[LogicalPlan] =
        List(input)

    override def toString(): String =
        s"Projection: ${ expr.map(_.toString()).mkString(", ") }"