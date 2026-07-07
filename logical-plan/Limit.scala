package logical

import datatypes.Schema

class Limit(
    val input: LogicalPlan,
    val limit: Int
) extends LogicalPlan:
    override def schema(): Schema = input.schema()
    override def children(): List[LogicalPlan] = List(input)
    override def toString(): String = s"Limit: $limit"