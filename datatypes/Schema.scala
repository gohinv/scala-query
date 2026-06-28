package datatypes

import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.types.pojo.Schema as ArrowSchema
import org.apache.arrow.vector.types.pojo.Field as ArrowField
import org.apache.arrow.vector.types.pojo.FieldType as ArrowFieldType

import scala.jdk.CollectionConverters.*

object SchemaConverter:
    def fromArrow(arrowSchema: ArrowSchema): Schema =
        val fields = arrowSchema.getFields.asScala.map { f =>
            Field(f.getName, f.getFieldType.getType, f.isNullable)
        }.toList
        Schema(fields)

case class Schema(fields: List[Field]):
    def toArrow(): ArrowSchema =
        val arrowFields = this.fields.map(_.toArrow()).asJava
        ArrowSchema(arrowFields)

case class Field(name: String, dataType: ArrowType, nullable: Boolean):
    def toArrow(): ArrowField =
        val fieldType = ArrowFieldType(nullable, dataType, None)
        ArrowField(name, fieldType, Nil)
