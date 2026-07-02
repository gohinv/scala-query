package datasource

import datatypes.RecordBatch
import datatypes.Schema

class InMemoryDataSource(val _schema: Schema, val data: List[RecordBatch]) extends DataSource:
    
    override def schema(): Schema = _schema
    override def scan(projection: List[String]): Iterable[RecordBatch] =
        if projection.isEmpty then
            data
        else
            val projectionIndices = projection.map( name =>
                _schema.fields.indexWhere(_.name == name)
            )
            val projectedSchema = _schema.select(projection)
            data.map (
                batch => RecordBatch(projectedSchema, projectionIndices.map( index => batch.field(index)))
            )
     