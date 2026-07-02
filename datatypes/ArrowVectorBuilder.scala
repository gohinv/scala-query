package datatypes

import java.math.BigDecimal
import org.apache.arrow.vector.* 

class ArrowVectorBuilder(val fieldVector: FieldVector):
    
    def set(i: Int, value: Any): Unit =
        fieldVector match {
            case vcv: VarCharVector =>
                if value == null then
                    vcv.setNull(i)
                else if value.isInstanceOf[Array[Byte]] then
                    vcv.set(i, value)
                else
                    vcv.set(i, value.toString.getBytes(StandardCharsets.UTF_8))
            case tv: TinyIntVector =>
                if value == null then tv.setNull(i)
                else value match {
                    case n: java.lang.Number => tv.set(i, n.byteValue())
                    case s: String           => tv.set(i, s.toByte)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case si: SmallIntVector =>
                if value == null then si.setNull(i)
                else value match {
                    case n: java.lang.Number => si.set(i, n.shortValue())
                    case s: String           => si.set(i, s.toShort)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case iv: IntVector =>
                if value == null then iv.setNull(i)
                else value match {
                    case n: java.lang.Number => iv.set(i, n.intValue())
                    case s: String           => iv.set(i, s.toInt)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case biv: BigIntVector =>
                if value == null then biv.setNull(i)
                else value match {
                    case n: java.lang.Number => biv.set(i, n.longValue())
                    case s: String           => biv.set(i, s.toLong)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case f4v: Float4Vector =>
                if value == null then f4v.setNull(i)
                else value match {
                    case n: java.lang.Number => f4v.set(i, n.floatValue())
                    case s: String           => f4v.set(i, s.toFloat)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case f8v: Float8Vector =>
                if value == null then f8v.setNull(i)
                else value match {
                    case n: java.lang.Number => f8v.set(i, n.doubleValue())
                    case s: String           => f8v.set(i, s.toDouble)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case decv: DecimalVector =>
                if value == null then decv.setNull(i)
                else 
                    val decimal = value match {
                        case bd: BigDecimal => bd
                        case n: java.lang.Number => BigDecimal(n.doubleValue())
                        case s: String => BigDecimal(s)
                        case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                    }
                    decv.set(i, decimal.setScale(decv.getScale, java.math.RoundingMode.HALF_UP))
            case d256v: Decimal256Vector =>
                if value == null then d256v.setNull(i)
                else 
                    val decimal = value match {
                        case bd: BigDecimal => bd
                        case n: java.lang.Number => BigDecimal(n.doubleValue())
                        case s: String => BigDecimal(s)
                        case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                    }
                    d256v.set(i, decimal.setScale(d256v.getScale, java.math.RoundingMode.HALF_UP))
            case ddv: DateDayVector =>
                if value == null then ddv.setNull(i)
                else value match {
                    case n: java.lang.Number => ddv.set(i, n.intValue())
                    case s: String           => ddv.set(i, s.toInt)
                    case _ => throw new IllegalArgumentException(s"Unsupported value type: ${value.getClass.getName}")
                }
            case other: FieldVector => throw new IllegalArgumentException(s"Unsupported vector type: ${other.getClass.getName}")
        }

    def setValueCount(count: Int): Unit =
        fieldVector.setValueCount(count)
    
    def build(): ColumnVector =
        ArrowFieldVector(fieldVector)