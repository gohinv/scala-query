package logical

import datatypes.Schema

/* Represents a data transformation that returns a relation (a set of tuples) */
trait LogicalPlan:

    /* Return schema of the data produced by the logical plan */
    def schema(): Schema

    /* Return the children (inputs) of this logical plan. Enables use of visitor pattern to walk query tree */
    def children(): List[LogicalPlan]

    def pretty(): String = format(this)

def format(plan: LogicalPlan, indent: Int = 0): String =
    val b = StringBuilder()
    (0 until indent).foreach(_ => b.append("\t"))
    b.append(plan.toString()).append("\n")
    plan.children().foreach( child => b.append(format(child, indent + 1)))
    b.toString()
        
    
