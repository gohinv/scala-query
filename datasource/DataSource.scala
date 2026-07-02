package datasource

import datatypes.Schema
import datatypes.RecordBatch
import scala.collection.immutable.LazyList

trait DataSource:

    // Return schema of data source
    def schema(): Schema

    // Scan data source selecting specified columns
    // use LazyList to stream data by batches
    def scan(projection: List[String]): Iterable[RecordBatch]
