package logical

import datatypes.Field
import datatypes.Schema 

enum JoinType:
    case Inner, Left, Right

class Join(
    val left: LogicalPlan,
    val right: LogicalPlan,
    val joinType: JoinType,
    val on: List[(String, String)]
) extends LogicalPlan:
    override def schema(): Schema =
        val duplicateKeys = on.filter(pair => pair(0) == pair(1)).map(pair => pair(0)).toSet
        val fields: List[Field] =
            joinType match {
                case JoinType.Inner | JoinType.Left =>
                    val leftFields = left.schema().fields
                    val rightFields = right.schema().fields.filter(
                        field => !duplicateKeys.contains(field.name)
                    )
                    leftFields ++ rightFields
                case logical.JoinType.Right =>
                    val leftFields = left.schema().fields.filter(
                        field => !duplicateKeys.contains(field.name)
                    )
                    val rightFields = right.schema().fields
                    leftFields ++ rightFields
            }
        Schema(fields)
    
    override def children(): List[LogicalPlan] =
        List(left, right)

    override def toString(): String =
        s"Join: type=$joinType, on=$on"


