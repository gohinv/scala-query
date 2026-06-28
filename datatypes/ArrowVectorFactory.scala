package datatypes

import java.lang.IllegalStateException
import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.*
import org.apache.arrow.vector.types.pojo.ArrowType

object ArrowAllocator:
    val rootAllocator: BufferAllocator = RootAllocator(Long.MaxValue)

object FieldVectorFactory:
    def create(arrowType: ArrowType, initialCapacity: Int): FieldVector =
        val fieldVector: FieldVector = arrowType match {
            case ArrowTypes.BooleanType => BitVector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.Int8Type => TinyIntVector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.Int16Type => SmallIntVector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.Int32Type => IntVector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.Int64Type => BigIntVector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.FloatType => Float4Vector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.DoubleType => Float8Vector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.StringType => VarCharVector("v", ArrowAllocator.rootAllocator)
            case ArrowTypes.DateDayType => DateDayVector("v", ArrowAllocator.rootAllocator)
        }
        if (initialCapacity > 0) then
            fieldVector.setInitialCapacity(initialCapacity)
        fieldVector.allocateNew()
        fieldVector
    
