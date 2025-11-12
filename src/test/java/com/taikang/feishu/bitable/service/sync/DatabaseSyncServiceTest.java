package com.taikang.feishu.bitable.service.sync;

import com.lark.oapi.service.bitable.v1.model.AppTableField;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordResp;
import com.lark.oapi.service.bitable.v1.model.CreateAppTableRespBody;
import com.taikang.feishu.bitable.service.record.BitableRecordService;
import com.taikang.feishu.bitable.service.table.BitableTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(DatabaseSyncService.class)
public class DatabaseSyncServiceTest {

    @Autowired
    private DatabaseSyncService databaseSyncService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private BitableTableService bitableTableService;

    @MockBean
    private BitableRecordService bitableRecordService;

    private static final String TEST_APP_TOKEN = "bascnTestAppToken";
    private static final String TEST_DB_TABLE_NAME = "employees";
    private static final String TEST_BITABLE_TABLE_NAME = "员工列表";
    private static final String NEW_TABLE_ID = "tblTestTableId";

    private List<AppTableField> mockFields;
    private List<AppTableRecord> mockAppRecords;

    @BeforeEach
    void setUp() throws Exception {
        // ... ( 模拟 readTableFields 的行为 - ) ...
        mockFields = new ArrayList<>();
        mockFields.add(AppTableField.newBuilder().fieldName("id").type(2).build()); // 数字
        mockFields.add(AppTableField.newBuilder().fieldName("name").type(1).build()); // 文本
        mockFields.add(AppTableField.newBuilder().fieldName("hire_date").type(5).build()); // 日期
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(mockFields);


        // ... (模拟 readTableData 的行为 - ) ...
        mockAppRecords = new ArrayList<>();
        Map<String, Object> recordFields = Map.of(
                "id", 1,
                "name", "张三",
                "hire_date", 946684800000L
        );
        mockAppRecords.add(AppTableRecord.newBuilder().fields(recordFields).build());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class)))
                .thenReturn(mockAppRecords);


        CreateAppTableRespBody createTableRespBody = new CreateAppTableRespBody();
        createTableRespBody.setTableId(NEW_TABLE_ID);
        when(bitableTableService.createTableWithFields(
                eq(TEST_APP_TOKEN),
                eq(TEST_BITABLE_TABLE_NAME),
                eq(mockFields)
        )).thenReturn(createTableRespBody);


        BatchCreateAppTableRecordResp mockRecordResp = new BatchCreateAppTableRecordResp();
        when(bitableRecordService.batchCreateRecords(
                anyString(),
                anyString(),
                anyList()
        )).thenReturn(mockRecordResp);


        when(jdbcTemplate.update(
                contains("INSERT INTO bitable_tables"),
                anyString(), anyString(), anyString(), anyString()
        )).thenReturn(1);
    }

    /**
     * 测试: 完整同步流程成功
     */
    @Test
    void testSyncTableToBitable_Success() throws Exception {
        // ... ( 执行 - ) ...
        String resultTableId = databaseSyncService.syncTableToBitable(
                TEST_APP_TOKEN,
                TEST_DB_TABLE_NAME,
                TEST_BITABLE_TABLE_NAME
        );

        // ... ( 验证 (Assert) - ) ...
        assertEquals(NEW_TABLE_ID, resultTableId, "返回的 TableId 应该与模拟创建的一致");

        // ... ( 验证 (Verify) - ) ...
        verify(bitableTableService, times(1)).createTableWithFields(
                TEST_APP_TOKEN,
                TEST_BITABLE_TABLE_NAME,
                mockFields
        );

        ArgumentCaptor<List<AppTableRecord>> recordsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bitableRecordService, times(1)).batchCreateRecords(
                eq(TEST_APP_TOKEN),
                eq(NEW_TABLE_ID),
                recordsCaptor.capture()
        );
        List<AppTableRecord> capturedRecords = recordsCaptor.getValue();
        assertEquals(1, capturedRecords.size());
        assertInstanceOf(Long.class, capturedRecords.get(0).getFields().get("hire_date"));


        // --- 验证 jdbcTemplate.update (保存日志) ---
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT INTO bitable_tables"),
                eq(NEW_TABLE_ID),
                eq(TEST_APP_TOKEN),
                eq(TEST_BITABLE_TABLE_NAME),
                eq(TEST_DB_TABLE_NAME)
        );
    }
}