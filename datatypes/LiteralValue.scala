package datatypes

import org.apache.arrow.vector.types.pojo.ArrowType

class LiteralValueVector(
    val arrowType: ArrowType,
    val value: Any,
    val size: Int
) extends ColumnVector:
    override def getType(): ArrowType = arrowType

    override def getValue(i: Int): Any =
        if (i < 0 || i >= size) then
            throw IndexOutOfBoundsException
        value

    override def size(): Int = size

    override def close(): Unit = ()
