package datasource

import datatypes.*
import javax.xml.crypto.Data
import os.*
import com.univocity.parsers.csv.*
import com.univocity.parsers.common.record.Record
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import datatypes.ArrowTypes
import java.util.NoSuchElementException
import scala.collection.mutable.ArrayBuffer
import org.apache.arrow.vector.*
import scala.collection.JavaConverters.*

class CSVDataSource(
    val filename: String,
    val providedSchema: Schema,
    hasHeaders: Boolean,
    batchSize: Int
) extends DataSource:
    lazy val finalSchema: Schema = Option(providedSchema).getOrElse(inferSchema())

    override def schema(): Schema = 
        finalSchema

    private def buildParser(settings: CsvParserSettings) =
        CsvParser(settings)

    private def defaultSettings(): CsvParserSettings =
        val s = CsvParserSettings()
        s.setDelimiterDetectionEnabled(true)
        s.setLineSeparatorDetectionEnabled(true)
        s.setSkipEmptyLines(true)
        s.setAutoClosingEnabled(true)
        s

    override def scan(projection: List[String]): Iterable[RecordBatch] =
        val path = os.pwd / filename
        if !os.exists(path) then throw FileNotFoundException(s"File not found: $filename")
        val readSchema = {
            if projection.isEmpty then finalSchema else finalSchema.select(projection)
        }
        val settings = defaultSettings()
        if projection.nonEmpty then settings.selectFields(projection*)
        settings.setHeaderExtractionEnabled(hasHeaders)
        if !hasHeaders then settings.setHeaders(readSchema.fields.map(_.name)*)

        val parser = buildParser(settings)
        parser.beginParsing(os.read.inputStream(path), StandardCharsets.UTF_8.name())
        parser.getDetectedFormat()

        ReaderAsSequence(readSchema, parser, batchSize)

    private def inferSchema(): Schema =
        val path = os.pwd / filename
        if !os.exists(path) then throw FileNotFoundException(s"File not found: $filename")
        val settings = defaultSettings()
        settings.setHeaderExtractionEnabled(hasHeaders)
        val parser = buildParser(settings)
        val stream = os.read.inputStream(path)
        try
            parser.beginParsing(stream, StandardCharsets.UTF_8.name())
            parser.getDetectedFormat()
            val firstRow = parser.parseNext()
            val schema = {
                if hasHeaders then
                    val names = Option(parser.getContext().parsedHeaders())
                        .map(_.filter(_ != null).toList)
                        .filter(_.nonEmpty)
                        .getOrElse(throw IllegalStateException("Expected headers in file but none found"))
                    Schema(names.map(colName => Field(colName, ArrowTypes.StringType, true)))
                else
                    Schema(firstRow.indices.map(i => Field(s"col_$i", ArrowTypes.StringType, true)).toList)
            }
            parser.stopParsing()
            schema
        finally
            stream.close()

class ReaderAsSequence(
    schema: Schema,
    parser: CsvParser,
    batchSize: Int
) extends Iterable[RecordBatch]:
    override def iterator: Iterator[RecordBatch] = 
        ReaderIterator(schema, parser, batchSize)

class ReaderIterator(
    schema: Schema,
    parser: CsvParser,
    batchSize: Int
) extends Iterator[RecordBatch]:
    private var pending = Option.empty[RecordBatch]
    private var started = false
    
    // Check if there are more batches of records to process
    override def hasNext: Boolean =
        if !started then
            started = true
            pending = Some(nextBatch())
        pending.nonEmpty

    // Get the next batch of records 
    override def next(): RecordBatch =
        if !started then hasNext
        val out = pending.getOrElse(throw NoSuchElementException("No more batches"))
        pending = Option(nextBatch())
        out

    // Parse a batch of records from the file into a RecordBatch
    private def nextBatch(): RecordBatch =
        val rows = new ArrayBuffer[Record](batchSize)
        var line = parser.parseNextRecord()
        while line != null && rows.size < batchSize do
            rows += line
            line = parser.parseNextRecord()
        if rows.isEmpty then null else createBatch(rows)

    // Create a RecordBatch from a list of records
    private def createBatch(rows: ArrayBuffer[Record]): RecordBatch =
        val root = VectorSchemaRoot.create(schema.toArrow(), ArrowAllocator.rootAllocator)
        root.getFieldVectors.asScala.zipWithIndex.foreach((fv, idx) => 
            fv match {
                case vcv: VarCharVector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    vcv.setSafe(i, valueStr.getBytes(StandardCharsets.UTF_8))
                )
                case tv: TinyIntVector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    if valueStr.nonEmpty then tv.setSafe(i, valueStr.toByte)
                    else tv.setNull(i)
                )
                case si: SmallIntVector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    if valueStr.nonEmpty then si.setSafe(i, valueStr.toShort)
                    else si.setNull(i)
                )
                case iv: IntVector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    if valueStr.nonEmpty then iv.setSafe(i, valueStr.toInt)
                    else iv.setNull(i)
                )
                case biv: BigIntVector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    if valueStr.nonEmpty then biv.setSafe(i, valueStr.toLong)
                    else biv.setNull(i)
                )
                case f4v: Float4Vector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    if valueStr.nonEmpty then f4v.setSafe(i, valueStr.toFloat)
                    else f4v.setNull(i)
                )
                case f8v: Float8Vector => rows.zipWithIndex.foreach( (row, i) => 
                    val valueStr: String = row.getValue(idx, "").trim()
                    if valueStr.nonEmpty then f8v.setSafe(i, valueStr.toDouble)
                    else f8v.setNull(i)
                )
                case other: FieldVector => throw IllegalStateException(s"Unsupported vector type: ${other.getClass.getName}")
            }
        )
        // construct and return record batch from parsed field vectors
        RecordBatch(schema, root.getFieldVectors.asScala.toList.map (fv => ArrowFieldVector(fv)))
    
        


        

