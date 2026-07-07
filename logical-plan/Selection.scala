package logical

import datatypes.Schema
import os.write.channel.over

/* Logical plan representing a selection (filter) against an input plan */

class Selection(val input: LogicalPlan, val predicate: LogicalExpr) extends LogicalPlan:
    override def schema(): Schema =
        input.schema()

    override def children(): List[LogicalPlan] =
        List(input)

    override def toString(): String =
        s"Selection: $predicate"