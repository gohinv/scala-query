package datatypes

import org.apache.arrow.vector.types.pojo.ArrowType
import java.lang.IndexOutOfBoundsException

class LiteralValueVector(
    val arrowType: ArrowType,
    val value: Any,
    val vecSize: Int
) extends ColumnVector:
    override def getType(): ArrowType = arrowType

    override def getValue(i: Int): Any =
        if (i < 0 || i >= vecSize) then
            throw new IndexOutOfBoundsException(s"Index out of bounds: $i")
        value

    override def size(): Int = vecSize

    override def close(): Unit = ()
