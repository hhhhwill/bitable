package com.taikang.feishu.bitable.service.sync;

import com.lark.oapi.service.bitable.v1.model.AppTableField;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.lark.oapi.service.bitable.v1.model.CreateAppTableRespBody;
import com.taikang.feishu.bitable.service.record.BitableRecordService;
import com.taikang.feishu.bitable.service.table.BitableTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseSyncService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSyncService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BitableTableService bitableTableService;

    @Autowired
    private BitableRecordService bitableRecordService;

    @Transactional
    public String syncTableToBitable(String appToken, String dbTableName, String newBitableTableName) throws Exception {
        log.info("开始读取数据库表结构: {}", dbTableName);
        List<AppTableField> fields = readTableFields(dbTableName);
        if (fields.isEmpty()) {
            throw new RuntimeException("无法读取表结构或表为空: " + dbTableName);
        }

        CreateAppTableRespBody tableResp = bitableTableService.createTableWithFields(appToken, newBitableTableName, fields);
        String newTableId = tableResp.getTableId();

        log.info("开始读取数据库内容: {}", dbTableName);
        List<AppTableRecord> records = readTableData(dbTableName, fields);

        if (!records.isEmpty()) {
            bitableRecordService.batchCreateRecords(appToken, newTableId, records);
        }

        log.info("开始将飞书表信息写回本地数据库...");
        saveSyncInfoToDatabase(appToken, newTableId, dbTableName, newBitableTableName);

        return newTableId;
    }

    /**
     * 辅助方法: 读取数据库表结构
     */
    private List<AppTableField> readTableFields(String dbTableName) {
        return jdbcTemplate.execute((ConnectionCallback<List<AppTableField>>) con -> {
            List<AppTableField> fields = new ArrayList<>();
            try {
                DatabaseMetaData metaData = con.getMetaData();
                try (ResultSet rs = metaData.getColumns(null, null, dbTableName, null)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        int dataType = rs.getInt("DATA_TYPE");

                        // --- 修正点 1: 保持您修改的 int 类型 (正确) ---
                        int fieldType = mapSqlTypeToBitableType(dataType);

                        fields.add(AppTableField.newBuilder()
                                .fieldName(columnName)
                                .type(fieldType) // .type(int) 是正确的
                                .build());
                    }
                }
            } catch (SQLException e) {
                log.error("读取数据库元数据失败, 表名: {}", dbTableName, e);
                throw new RuntimeException("无法读取数据库元数据: " + e.getMessage(), e);
            }
            return fields;
        });
    }

    /**
     * 辅助方法: 读取数据库数据
     */
    private List<AppTableRecord> readTableData(String dbTableName, List<AppTableField> fields) {
        String sql = "SELECT * FROM " + dbTableName;

        return jdbcTemplate.query(sql, (ResultSet rs) -> {
            List<AppTableRecord> records = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> fieldMap = new HashMap<>();
                for (AppTableField field : fields) {
                    String fieldName = field.getFieldName();
                    Object value = rs.getObject(fieldName);

                    if (value != null) {

                        if (field.getType() == 5L) {

                            if (value instanceof Date) {
                                fieldMap.put(fieldName, ((Date) value).getTime());
                            } else {
                                log.warn("无法转换的日期类型: {} for field {}", value.getClass(), fieldName);
                            }
                        } else {
                            fieldMap.put(fieldName, value);
                        }
                    }
                }
                records.add(AppTableRecord.newBuilder()
                        .fields(fieldMap)
                        .build());
            }
            return records;
        });
    }

    /**
     * 辅助方法: 映射 SQL 类型到飞书字段类型
     */
    private int mapSqlTypeToBitableType(int sqlType) {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.DECIMAL:
                return 2;

            case Types.DATE:
            case Types.TIMESTAMP:
                return 5;

            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            default:
                return 1;
        }
    }

    /**
     * 辅助方法: 将同步信息存入本地数据库
     */
    private void saveSyncInfoToDatabase(String appToken, String tableId, String dbTableName, String bitableTableName) {
        String sql = "INSERT INTO synced_tables (db_table_name, bitable_app_token, bitable_table_id, bitable_table_name) " +
                "VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(sql, dbTableName, appToken, tableId, bitableTableName);
        log.info("同步信息已保存到本地数据库: {}", dbTableName);
    }
}