package execution

import logical.*
import datasource.CSVDataSource
import datasource.ParquetDataSource

class ExecutionContext:
    def csv(filename: String): DataFrame =
        DataFrameImpl(Scan(filename, CSVDataSource(filename, null, true, 100), List.empty))

    def parquet(filename: String): DataFrame =
        DataFrameImpl(Scan(filename, ParquetDataSource(filename), List.empty))