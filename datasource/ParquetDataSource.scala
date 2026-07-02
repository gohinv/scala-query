package datasource

import datatypes.*
import java.math.BigDecimal
import org.apache.arrow.vector.BigIntVector
import org.apache.arrow.vector.BitVector
import org.apache.arrow.vector.DecimalVector
import org.apache.arrow.vector.Float4Vector
import org.apache.arrow.vector.Float8Vector
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.VarBinaryVector
import org.apache.arrow.vector.VarCharVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.types.pojo.Schema as ArrowSchema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.arrow.schema.SchemaConverter as ParquetSchemaConverter
import org.apache.parquet.column.page.PageReadStore
import org.apache.parquet.example.data.Group
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.io.ColumnIOFactory
import org.apache.parquet.schema.PrimitiveType
import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

class ParquetDataSource(val filename: String) extends DataSource:
    override def schema(): Schema =
       val scan = ParquetScan(filename, List.empty)
       val arrowSchema = new ParquetSchemaConverter().fromParquet(scan.schema).getArrowSchema
       datatypes.SchemaConverter.fromArrow(arrowSchema)
    override def scan(projection: List[String]): Iterable[RecordBatch] =
        ParquetScan(filename, projection)

class ParquetScan(val filename: String, columns: List[String]) extends AutoCloseable, Iterable[RecordBatch]:
    private val reader = ParquetFileReader.open(HadoopInputFile.fromPath(Path(filename), new Configuration))
    val schema = reader.getFooter.getFileMetaData.getSchema
    override def iterator(): Iterator[RecordBatch] =
        ParquetIterator(reader, columns)
    override def close(): Unit = reader.close()

class ParquetIterator(reader: ParquetFileReader, projectedColumns: List[String]) extends Iterator[RecordBatch]:
    val parquetSchema = reader.getFooter.getFileMetaData.getSchema
    val arrowSchema = new ParquetSchemaConverter().fromParquet(parquetSchema).getArrowSchema
    val projectedArrowSchema = ArrowSchema(
        projectedColumns.map(colName => arrowSchema.getFields.asScala.find(f => 
            f.getName == colName).getOrElse(throw new IllegalArgumentException(s"Column not found: $colName"))
        ).asJava
    )
    var batch: Option[RecordBatch] = None

    override def hasNext(): Boolean =
        batch = Some(nextBatch())
        batch.nonEmpty

    override def next(): RecordBatch =
        val next = batch.getOrElse(throw new NoSuchElementException("No more batches"))
        batch = None
        next
    
    private def nextBatch(): RecordBatch =
        val pages: PageReadStore = reader.readNextRowGroup()
        if pages == null then 
            return null
        if pages.getRowCount > Int.MaxValue then throw new IllegalStateException("Row count exceeds Int.MaxValue")

        val rowCnt = pages.getRowCount.toInt
        println(s"Processing $rowCnt rows")
        val root = VectorSchemaRoot.create(projectedArrowSchema, ArrowAllocator.rootAllocator)
        root.allocateNew()
        root.setRowCount(rowCnt)
        val columnIO = new ColumnIOFactory().getColumnIO(parquetSchema)
        val recordReader = columnIO.getRecordReader(pages, GroupRecordConverter(parquetSchema))
        for rowIdx <- 0 until rowCnt do
            val group: Group = recordReader.read()
            for projectionIdx <- 0 until projectedColumns.size do
                val fieldName = projectedColumns(projectionIdx)
                val fieldType = parquetSchema.getType(parquetSchema.getFieldIndex(fieldName))
                val vector = root.getFieldVectors.get(projectionIdx)
                    if group.getFieldRepetitionCount(fieldName) == 1 then
                        fieldType.asPrimitiveType().getPrimitiveTypeName() match {
                            case PrimitiveType.PrimitiveTypeName.BOOLEAN =>
                                vector.asInstanceOf[BitVector].set(rowIdx, if group.getBoolean(fieldName, 0) then 1 else 0)
                            case PrimitiveType.PrimitiveTypeName.INT32 =>
                                vector.asInstanceOf[IntVector].set(rowIdx, group.getInteger(fieldName, 0))
                            case PrimitiveType.PrimitiveTypeName.INT64 =>
                                val longValue = group.getLong(fieldName, 0)
                                vector match {
                                    case biv: BigIntVector => biv.set(rowIdx, longValue)
                                    case dv: DecimalVector =>
                                        // Parquet stores decimals as unscaled integers, need to apply scale from Arrow vector
                                        val unscaled = BigDecimal.valueOf(longValue, dv.getScale)
                                        dv.set(rowIdx, unscaled)
                                    case _ => throw new IllegalStateException(s"Unsupported vector type for INT64: ${vector.getClass.getName}")
                                }
                            case PrimitiveType.PrimitiveTypeName.FLOAT =>
                                vector.asInstanceOf[Float4Vector].set(rowIdx, group.getFloat(fieldName, 0))
                            case PrimitiveType.PrimitiveTypeName.DOUBLE =>
                                vector.asInstanceOf[Float8Vector].set(rowIdx, group.getDouble(fieldName, 0))
                            case PrimitiveType.PrimitiveTypeName.BINARY =>
                                vector.asInstanceOf[VarBinaryVector].set(rowIdx, group.getBinary(fieldName, 0).getBytes)
                            case PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY =>
                                val bytes = group.getBinary(fieldName, 0).getBytes
                                vector match {
                                    case vcv: VarCharVector => vcv.setSafe(rowIdx, bytes)
                                    case vbv: VarBinaryVector => vbv.setSafe(rowIdx, bytes)
                                    case _ => throw new IllegalStateException(s"Unsupported vector type for FIXED_LEN_BYTE_ARRAY: ${vector.getClass.getName}")
                                }
                            case PrimitiveType.PrimitiveTypeName.INT96 =>
                                val bytes = group.getInt96(fieldName, 0).getBytes
                                vector.asInstanceOf[VarBinaryVector].setSafe(rowIdx, bytes)
                            case _ => throw new IllegalStateException(s"Unsupported primitive type: ${fieldType.asPrimitiveType().getPrimitiveTypeName()}")
                        }
        val schema = datatypes.SchemaConverter.fromArrow(projectedArrowSchema)
        RecordBatch(schema, root.getFieldVectors.asScala.map(ArrowFieldVector(_)).toList)
