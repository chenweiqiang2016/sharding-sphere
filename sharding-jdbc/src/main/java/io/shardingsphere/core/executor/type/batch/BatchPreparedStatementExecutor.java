/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.executor.type.batch;

import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.executor.BaseStatementUnit;
import io.shardingsphere.core.executor.ExecuteCallback;
import io.shardingsphere.core.executor.ExecutorEngine;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PreparedStatement Executor for  multiple threads to process add batch.
 * 
 * @author zhangliang
 * @author maxiaoguang
 */
@RequiredArgsConstructor
public final class BatchPreparedStatementExecutor {
    
    private final ExecutorEngine executorEngine;
    
    private final DatabaseType dbType;
    
    private final SQLType sqlType;
    
    private final Collection<BatchPreparedStatementUnit> batchPreparedStatementUnits;
    
    private final int batchCount;
    
    /**
     * Execute batch.
     * 
     * @return execute results
     * @throws SQLException SQL exception
     */
    public int[] executeBatch() throws SQLException {
        return accumulate(executorEngine.execute(batchPreparedStatementUnits, new ExecuteCallback<int[]>() {
            
            @Override
            public int[] execute(final BaseStatementUnit baseStatementUnit) throws Exception {
                return baseStatementUnit.getStatement().executeBatch();
            }
            
            @Override
            public SQLType getSQLType() {
                return sqlType;
            }
        }));
    }
    
    private int[] accumulate(final List<int[]> results) {
        int[] result = new int[batchCount];
        int count = 0;
        for (BatchPreparedStatementUnit each : batchPreparedStatementUnits) {
            for (Map.Entry<Integer, Integer> entry : each.getJdbcAndActualAddBatchCallTimesMap().entrySet()) {
                int value = null == results.get(count) ? 0 : results.get(count)[entry.getValue()];
                if (DatabaseType.Oracle == dbType) {
                    result[entry.getKey()] = value;
                } else {
                    result[entry.getKey()] += value;
                }
            }
            count++;
        }
        return result;
    }
}
