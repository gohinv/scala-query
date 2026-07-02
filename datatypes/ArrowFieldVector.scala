package datatypes

import java.lang.IllegalStateException

import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.FieldVector
import org.apache.arrow.vector.*
import scala.util.FromDigits.Decimal

trait ColumnVector extends AutoCloseable:
    def getType(): ArrowType
    def getValue(i: Int): Any
    def size(): Int

class ArrowFieldVector(val fv: FieldVector) extends ColumnVector:
    override def getType(): ArrowType = 
        fv match {
            case _: BitVector => ArrowTypes.BooleanType
            case _: TinyIntVector => ArrowTypes.Int8Type
            case _: SmallIntVector => ArrowTypes.Int16Type
            case _: IntVector => ArrowTypes.Int32Type
            case _: BigIntVector => ArrowTypes.Int64Type
            case _: Float4Vector => ArrowTypes.FloatType
            case _: Float8Vector => ArrowTypes.DoubleType
            case _: VarCharVector => ArrowTypes.StringType
            case _: VarBinaryVector => ArrowTypes.BinaryType
            case _: DateDayVector => ArrowTypes.DateDayType
            case _: DecimalVector => fv.getField.getType
            case _: Decimal256Vector => fv.getField.getType
            case other => throw new IllegalStateException(s"Unsupported field type: ${other.getClass.getName}")
        }
    override def getValue(i: Int): Any = 

        if (fv.isNull(i)) return null

        fv match {
            case bv: BitVector => (bv.get(i) == 1)
            case tv: TinyIntVector => tv.get(i)
            case si: SmallIntVector => si.get(i)
            case iv: IntVector => iv.get(i)
            case bi: BigIntVector => bi.get(i)
            case f4v: Float4Vector => f4v.get(i)
            case f8v: Float8Vector => f8v.get(i)
            case vcv: VarCharVector => {
                val bytes = vcv.get(i)
                if (bytes == null) null else String(bytes)
            }
            case vbv: VarBinaryVector => {
                val bytes = vbv.get(i)
                if (bytes == null) null else bytes
            }
            case ddv: DateDayVector => ddv.get(i)
            case dv: DecimalVector => dv.getObject(i)
            case d256v: Decimal256Vector => d256v.getObject(i)
            case other => throw new IllegalStateException(s"Unsupported field type: ${other.getClass.getName}")
        }

    override def size(): Int = fv.getValueCount()

    override def close(): Unit = fv.close()



    