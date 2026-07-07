package logical

import datatypes.ArrowTypes
import datatypes.Field
import java.sql.SQLException
import org.apache.arrow.vector.types.pojo.ArrowType

/* Reference a column by name */
class Column(val name: String) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        input.schema().fields.find(_.name == name).getOrElse(throw SQLException(s"Column not found: $name"))

    override def toString(): String =
        s"#$name"

def column(name: String): Column = Column(name)

/* Reference a column by index */
class ColumnIndex(val index: Int) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        input.schema().fields(index)
    
    override def toString(): String =
        s"$index"

/* LITERAL EXPRESSIONS */

/* Literal string value */
class LiteralString(val string: String) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(string, ArrowTypes.StringType, nullable = true)
    
    override def toString(): String =
        s"'$string'"

/* Convenience method to create a LiteralString */
def lit(string: String): LiteralString = LiteralString(string)

/* Literal long value */
class LiteralLong(val long: Long) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(long.toString, ArrowTypes.Int64Type, nullable = true)
    
    override def toString(): String =
        long.toString

/* Convenience method to create a LiteralLong */
def lit(long: Long): LiteralLong = LiteralLong(long)

/* Literal float value */
class LiteralFloat(val float: Float) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(float.toString, ArrowTypes.FloatType, nullable = true)
    
    override def toString(): String =
        float.toString

/* Convenience method to create a LiteralFloat */
def lit(float: Float): LiteralFloat = LiteralFloat(float)

/* Literal double value */
class LiteralDouble(val double: Double) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(double.toString, ArrowTypes.DoubleType, nullable = true)
    
    override def toString(): String =
        double.toString

/* Convenience method to create a LiteralDouble */
def lit(double: Double): LiteralDouble = LiteralDouble(double)

/* Literal date value */
class LiteralDate(val date: java.time.LocalDate) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(date.toString, ArrowTypes.DateDayType, nullable = true)
    
    override def toString(): String =
        s"DATE '$date'"

/* Convenience method to create a LiteralDate */
def lit(date: java.time.LocalDate): LiteralDate = LiteralDate(date)

/* Literal interval days value */
class LiteralIntervalDays(val days: Long) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field("$days days", ArrowTypes.IntervalDayTimeType, nullable = true)
    
    override def toString(): String =
        s"INTERVAL '$days DAYS'"

/* Date subtract interval expression */
class DateSubtractInterval(val dateExpr: LogicalExpr, val intervalExpr: LogicalExpr) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field("date_subtract_interval", ArrowTypes.DateDayType, nullable = true)
    
    override def toString(): String =
        s"$dateExpr - $intervalExpr"

/* Cast expression to a different data type */
class CastExpr(val expr: LogicalExpr, val dataType: ArrowType) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(expr.toField(input).name, dataType, nullable = true)
    
    override def toString(): String =
        s"CAST($expr AS $dataType)"

/* Convenience method to create a CastExpr */
def cast(expr: LogicalExpr, dataType: ArrowType): CastExpr = CastExpr(expr, dataType)

/* ARITHMETIC AND LOGICAL EXPRESSIONS */

abstract class BinaryExpr(
    val name: String,
    val op: String,
    val left: LogicalExpr,
    val right: LogicalExpr
) extends LogicalExpr:
    override def toString(): String =
        s"$left $op $right"

abstract class UnaryExpr(
    val name: String,
    val op: String,
    val expr: LogicalExpr
) extends LogicalExpr:
    override def toString(): String =
        s"$op $expr"

/* Logical NOT expression */
class Not(expr: LogicalExpr) extends UnaryExpr("not", "NOT", expr):
    override def toField(input: LogicalPlan): Field =
        Field(expr.toField(input).name, ArrowTypes.BooleanType, nullable = true)

/* Binary expressions that return a boolean result */
abstract class BooleanBinaryExpr(
    name: String,
    op: String,
    left: LogicalExpr,
    right: LogicalExpr
) extends BinaryExpr(name, op, left, right):
    override def toField(input: LogicalPlan): Field =
        Field(name, ArrowTypes.BooleanType, nullable = true)

class Eq(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("eq", "=", left, right)

class Neq(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("neq", "!=", left, right)

class Gt(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("gt", ">", left, right)

class GtEq(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("gteq", ">=", left, right)

class Lt(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("lt", "<", left, right)

class LtEq(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("lteq", "<=", left, right)

class And(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("and", "AND", left, right)

class Or(left: LogicalExpr, right: LogicalExpr) extends BooleanBinaryExpr("or", "OR", left, right)

/* Convenience methods with infix operators */

extension (lhs: LogicalExpr)
  infix def ===(rhs: LogicalExpr): LogicalExpr = Eq(lhs, rhs)
  infix def neq(rhs: LogicalExpr): LogicalExpr = Neq(lhs, rhs)
  infix def gt(rhs: LogicalExpr): LogicalExpr = Gt(lhs, rhs)
  infix def gteq(rhs: LogicalExpr): LogicalExpr = GtEq(lhs, rhs)
  infix def lt(rhs: LogicalExpr): LogicalExpr = Lt(lhs, rhs)
  infix def lteq(rhs: LogicalExpr): LogicalExpr = LtEq(lhs, rhs)
  infix def and(rhs: LogicalExpr): LogicalExpr = And(lhs, rhs)
  infix def or(rhs: LogicalExpr): LogicalExpr = Or(lhs, rhs)

/* Arithmetic binary expressions */
abstract class MathExpr(
    name: String,
    op: String,
    left: LogicalExpr,
    right: LogicalExpr
) extends BinaryExpr(name, op, left, right):
    override def toField(input: LogicalPlan): Field =
        Field(name, left.toField(input).dataType, nullable = true)

class Add(left: LogicalExpr, right: LogicalExpr) extends MathExpr("add", "+", left, right)

class Subtract(left: LogicalExpr, right: LogicalExpr) extends MathExpr("sub", "-", left, right)

class Multiply(left: LogicalExpr, right: LogicalExpr) extends MathExpr("mul", "*", left, right)

class Divide(left: LogicalExpr, right: LogicalExpr) extends MathExpr("div", "/", left, right)

class Modulus(left: LogicalExpr, right: LogicalExpr) extends MathExpr("mod", "%", left, right)

/* Convenience methods with infix operators */

extension (lhs: LogicalExpr)
  infix def add(rhs: LogicalExpr): LogicalExpr = Add(lhs, rhs)
  infix def sub(rhs: LogicalExpr): LogicalExpr = Subtract(lhs, rhs)
  infix def mul(rhs: LogicalExpr): LogicalExpr = Multiply(lhs, rhs)
  infix def div(rhs: LogicalExpr): LogicalExpr = Divide(lhs, rhs)
  infix def mod(rhs: LogicalExpr): LogicalExpr = Modulus(lhs, rhs)

/* Aliased expression */

class Alias(val expr: LogicalExpr, val alias: String) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(alias, expr.toField(input).dataType, nullable = true)

    override def toString(): String =
        s"$expr AS $alias"
    
extension (expr: LogicalExpr)
    infix def alias(alias: String): Alias = Alias(expr, alias)

/* AGGREGATE EXPRESSIONS */

/* Base class for aggregate expresions */

abstract class AggregateExpr(
    val name: String,
    val expr: LogicalExpr
) extends LogicalExpr:
    override def toField(input: LogicalPlan): Field =
        Field(name, expr.toField(input).dataType, nullable = true)

    override def toString(): String =
        s"$name($expr)"

class Sum(expr: LogicalExpr) extends AggregateExpr("SUM", expr)

class Min(expr: LogicalExpr) extends AggregateExpr("MIN", expr)

class Max(expr: LogicalExpr) extends AggregateExpr("MAX", expr)

class Avg(expr: LogicalExpr) extends AggregateExpr("AVG", expr)

class Count(expr: LogicalExpr) extends AggregateExpr("COUNT", expr):
    override def toField(input: LogicalPlan): Field =
        Field("COUNT", ArrowTypes.Int32Type, nullable = false)

    override def toString(): String =
        s"COUNT($expr)"

class CountDistinct(expr: LogicalExpr) extends AggregateExpr("COUNT DISTINCT", expr):
    override def toField(input: LogicalPlan): Field =
        Field("COUNT DISTINCT", ArrowTypes.Int32Type, nullable = false)

    override def toString(): String =
        s"COUNT DISTINCT($expr)"



