package datatypes

import scala.collection.mutable.StringBuilder
import scala.compiletime.ops.double

class RecordBatch(val schema: Schema, val fields: List[ColumnVector]) extends AutoCloseable:
    def rowCount(): Int = fields.head.size()

    def columnCount(): Int = fields.size

    def field(i: Int): ColumnVector = fields(i)

    def toCSV(): String =
        val sb = StringBuilder()
        val columnCount = schema.fields.size
        for rowIndex <- 0 until rowCount() do
            for columnIndex <- 0 until columnCount do
                if columnIndex > 0 then
                    sb.append(",")
                val v = fields(columnIndex).getValue(rowIndex)
                if v == null then
                    sb.append("null")
                else if v.isInstanceOf[Array[Byte]] then
                    sb.append(String(v.asInstanceOf[Array[Byte]]))
                else
                    sb.append(v)
            sb.append("\n")
        sb.toString()
        
    override def toString(): String = toCSV()
    override def close(): Unit = fields.foreach(_.close)




 