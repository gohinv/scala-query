package logical

import datasource.DataSource
import datatypes.Schema
import sttp.ws.WebSocketFrame.Data

/* Scan of a data source */

class Scan(
    val path: String,
    val dataSource: DataSource,
    val projection: List[String]
) extends LogicalPlan:

    val _schema = deriveSchema()

    override def schema(): Schema = _schema

    private def deriveSchema(): Schema =
        val schema = dataSource.schema()
        if projection.isEmpty then
            schema
        else
            schema.select(projection)

    override def children(): List[LogicalPlan] = List.empty

    override def toString(): String =
        if projection.isEmpty then
            s"Scan: $path; projection=None"
        else
            s"Scan: $path; projection=${projection.mkString(", ")}"


        

