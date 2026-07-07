package logical

import datatypes.Schema

/* Logical plan representing an aggregate query against an input plan */

class Aggregate(
    val input: LogicalPlan,
    val groupExpr: List[LogicalExpr],
    val aggregateExpr: List[AggregateExpr]
) extends LogicalPlan:
    override def schema(): Schema =
        Schema(groupExpr.map(_.toField(input)) ++ aggregateExpr.map(_.toField(input)))

    override def children(): List[LogicalPlan] =
        List(input)

    override def toString(): String =
        s"Aggregate: groupExpr=$groupExpr, aggregateExpr=$aggregateExpr"
