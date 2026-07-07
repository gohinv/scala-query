package logical

import datatypes.Schema

trait DataFrame:
    def project(expr: List[LogicalExpr]): DataFrame
    def filter(expr: LogicalExpr): DataFrame
    def aggregate(groupBy: List[LogicalExpr], aggregate: List[AggregateExpr]): DataFrame
    def limit(limit: Int): DataFrame
    def join(right: DataFrame, joinType: JoinType, on: List[(String, String)]): DataFrame

    def schema(): Schema
    def logicalPlan(): LogicalPlan

class DataFrameImpl(plan: LogicalPlan) extends DataFrame:
    override def project(expr: List[LogicalExpr]): DataFrame =
        DataFrameImpl(Projection(plan, expr))
    override def filter(expr: LogicalExpr): DataFrame =
        DataFrameImpl(Selection(plan, expr))
    override def aggregate(groupBy: List[LogicalExpr], aggregate: List[AggregateExpr]): DataFrame =
        DataFrameImpl(Aggregate(plan, groupBy, aggregate))
    override def limit(limit: Int): DataFrame =
        DataFrameImpl(Limit(plan, limit))
    override def join(right: DataFrame, joinType: JoinType, on: List[(String, String)]): DataFrame =
        DataFrameImpl(Join(plan, right.logicalPlan(), joinType, on))
    
    override def schema(): Schema = plan.schema()
    override def logicalPlan(): LogicalPlan = plan
