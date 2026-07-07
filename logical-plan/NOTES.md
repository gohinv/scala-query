# Logical Plans and Expressions

First step after parsing a query is building a logical plan, which is a tree structure that represents the computation without specifying the physical operations needed to execute it.

## Logical Plans

### Advantage Of Logical Plan

1. Validation of query (column existence, type checking)
2. Optimization: Easily transform logical plan to make it more efficient
3. Flexibility: Same logical plan can execute differently based on data size or available resources

### Logical Plan Interface

[LogicalPlan.scala](LogicalPlan.scala)

- Logical Plan = Relation (set of rows with known schema)
- Each plan can have child plans as inputs --> Tree structure
- scan has no children (reads from data source), filter has 1 child (input expression), join has 2 children (left and right)
- children() enables walking plan tree

## Logical Expressions

- Logical plan = data flow between operators
- Expression = individual computation / operator / value
- Examples: column reference, literal value, math expression, comparison, boolean, aggregation, etc
- Nested expressions form trees

### LogicalExpr Interface

[LogicalExpr.scala](LogicalExpr.scala)

- `toField()`: during planning, need to know what type of value expr produces
- return name and data type of expression output



## LogicalExpr subclasses



### Column Expression

- Simplest expression: just reference a column by name
- `toField()`: just look up column name in input logical plan schema to ensure existence

### Literal Expression

- Represent literal value

### Binary Expressions

- Superclass of comparison, boolean logical, and arithmetic operators
- Comparison and logical expressions always produce boolean results
- Math expressions: preserve data type of left operand (simplified, we could handle promotion)

### Aggregate expressions

- Reduce multiple rows to one value (`SUM`, `MIN`, `MAX`, `AVG`, `COUNT`)
- Most aggregates return same type as their input expression 
- `COUNT` different, always returns integer count of rows

### Aliased Expressions

- SQL `AS` keyword: `expr AS alias`
- basically just changes the name of input but preserves type

## Logical Plan Subclasses

### Scan

- Leaf node in a query tree: read from data source; place where data enters a plan
- `projection` parameter selects columns

### Selection (Filter)

- Keep only rows where expression evaluates to true (SQL `WHERE` clause)
- Schema passes unchanged

### Projection

- Compute new columns from expression (SQL `SELECT c1, c2, ...`)
- Output schema is thus the selected subset of the input schema

### Aggregate

- Group by rows and compute aggregate functions (SQL `GROUP BY`)

### Join

- Combine rows from two inputs based on join keys
- `on` param specifies pairs of column names to join on

### Putting It Together

Example SQL Query:

```sql
SELECT name, salary * 1.1 AS new_salary
FROM employees
WHERE department='Engineering'
```

We build it bottom up using our expression API: 

```scala
// every query starts with a Scan
val scan = Scan("employees", employeeDataSource, List.empty) // no projection

// need to select only certain rows (WHERE)
val filter = Selection(
    scan, // use the leaf data source scan as the input logical plan
    Eq(Column("department"), LiteralString("Engineering")) // predicate logical expression consisting of Eq of col reference and literal string
)

//  SELECT statement
val project = Projection(
    filter,
    List(
        Column("name"),
        Alias(Multiply(Column("salary"), LiteralDouble(1.1)), "new_salary") // AS statement
    )
)
```

The logical plan will then get printed like so:
```
Projection: #name, #salary * 1.1 as new_salary
  Filter: #department = 'Engineering'
    Scan: employees; projection=None
```

# DataFrame API

- The method of building a logical plan shown above is verbose. We have to construct each piece separately and wire them together.
- Slight improvement by nesting the constructors.

### DataFrame Approach

DataFrame Interface: [DataFrame.scala](DataFrame.scala)

- A dataframe wraps a logical plan and provides transformation methods that return new logical plans
- Each method call adds a node to the plan
- Result: fluent API where code reads top-to-bottom in execution order
