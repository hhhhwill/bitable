package com.taikang.feishu.bitable.service.sync;

import com.lark.oapi.service.bitable.v1.model.AppTableField;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordResp; // 1. 导入
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
import org.springframework.jdbc.core.ResultSetExtractor; // 2. 导入

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 这是 DatabaseSyncService 的一个集成测试示例。
 *
 * @SpringBootTest 会加载完整的 Spring 应用上下文,
 * 这使我们能够 @Autowired 注入真正的 DatabaseSyncService。
 * @MockBean     会替换 Spring 上下文中的真实 Bean 为 Mockito 模拟对象。
 */
@SpringBootTest
@Import(DatabaseSyncService.class) // 显式导入我们要测试的 Service
public class DatabaseSyncServiceTest {

    // 1. 注入我们要测试的目标类
    @Autowired
    private DatabaseSyncService databaseSyncService;

    // 2. 模拟(Mock)所有外部依赖
    // 我们不希望测试真正调用数据库或飞书 API
    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private BitableTableService bitableTableService;

    @MockBean
    private BitableRecordService bitableRecordService;

    // 3. 定义测试中使用的常量
    private static final String TEST_APP_TOKEN = "bascnTestAppToken";
    private static final String TEST_DB_TABLE_NAME = "employees";
    private static final String TEST_BITABLE_TABLE_NAME = "员工列表";
    private static final String NEW_TABLE_ID = "tblTestTableId";

    // 4. 定义通用的模拟数据
    private List<AppTableField> mockFields;
    private List<AppTableRecord> mockAppRecords; // 修正点：这是 readTableData 应该返回的最终结果

    /**
     * @BeforeEach 在每个 @Test 方法运行前执行
     * 用于设置通用的模拟行为
     */
    @BeforeEach
    void setUp() throws Exception {
        // --- 模拟 readTableFields 的行为 ---
        // 4.1 准备 "readTableFields" 应该返回的模拟字段
        mockFields = new ArrayList<>();
        mockFields.add(AppTableField.newBuilder().fieldName("id").type(2).build()); // 数字
        mockFields.add(AppTableField.newBuilder().fieldName("name").type(1).build()); // 文本
        mockFields.add(AppTableField.newBuilder().fieldName("hire_date").type(5).build()); // 日期

        // 4.2 "告诉" Mockito: 当 jdbcTemplate.execute(ConnectionCallback) 被调用时,
        //     (readTableFields 使用的是 execute)
        //     返回我们上面准备的 mockFields 列表。
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(mockFields);


        // --- 模拟 readTableData 的行为 (【关键修正点】) ---
        // 4.3 准备 "readTableData" 应该返回的【最终结果】
        //     (即，日期已经被转换为 Long)
        mockAppRecords = new ArrayList<>();
        Map<String, Object> recordFields = Map.of(
                "id", 1,
                "name", "张三",
                "hire_date", 946684800000L // 2000-01-01 (已转换为毫秒)
        );
        mockAppRecords.add(AppTableRecord.newBuilder().fields(recordFields).build());

        // 4.4 "告诉" Mockito: 当 jdbcTemplate.query(sql, ResultSetExtractor) 被调用时,
        //     (readTableData 使用的是 query(sql, ResultSetExtractor))
        //     返回我们上面准备的 mockAppRecords 列表。
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class)))
                .thenReturn(mockAppRecords);


        // --- 模拟 bitableTableService 的行为 ---
        // 4.5 准备 "createTableWithFields" 应该返回的模拟响应
        CreateAppTableRespBody createTableRespBody = new CreateAppTableRespBody();
        createTableRespBody.setTableId(NEW_TABLE_ID); // 设置返回的新表 ID

        // 4.6 "告诉" Mockito: 当 bitableTableService.createTableWithFields(...) 被调用时,
        //     返回我们准备的 createTableRespBody
        when(bitableTableService.createTableWithFields(
                eq(TEST_APP_TOKEN),
                eq(TEST_BITABLE_TABLE_NAME),
                eq(mockFields)
        )).thenReturn(createTableRespBody);


        // --- 模拟 bitableRecordService 的行为 ---
        // 4.7 修正：batchCreateRecords 不是 void, 它返回一个 Resp 对象
        BatchCreateAppTableRecordResp mockRecordResp = new BatchCreateAppTableRecordResp();
        when(bitableRecordService.batchCreateRecords(
                anyString(),
                anyString(),
                anyList()
        )).thenReturn(mockRecordResp);


        // --- 模拟 saveSyncInfoToDatabase 的行为 ---
        // 4.8 "告诉" Mockito: 当 jdbcTemplate.update(...) (用于保存日志) 被调用时,
        //     返回 1 (代表1行被更新)
        when(jdbcTemplate.update(
                contains("INSERT INTO synced_tables"), // 确保 SQL 是插入日志的
                anyString(), anyString(), anyString(), anyString()
        )).thenReturn(1);
    }

    /**
     * 测试: 完整同步流程成功
     */
    @Test
    void testSyncTableToBitable_Success() throws Exception {
        // --- 5. 执行 ---
        // 调用我们要测试的真实方法
        String resultTableId = databaseSyncService.syncTableToBitable(
                TEST_APP_TOKEN,
                TEST_DB_TABLE_NAME,
                TEST_BITABLE_TABLE_NAME
        );

        // --- 6. 验证 (Assert) ---
        // 6.1 验证返回结果是否符合预期
        assertEquals(NEW_TABLE_ID, resultTableId, "返回的 TableId 应该与模拟创建的一致");

        // --- 7. 验证 (Verify) ---
        // 验证我们的模拟对象是否被正确调用了

        // 7.1 验证 bitableTableService.createTableWithFields 被调用了 1 次
        verify(bitableTableService, times(1)).createTableWithFields(
                TEST_APP_TOKEN,
                TEST_BITABLE_TABLE_NAME,
                mockFields
        );

        // 7.2 验证 bitableRecordService.batchCreateRecords 被调用了 1 次
        //     我们使用 ArgumentCaptor 来 "捕获" 传入的参数, 以便检查
        ArgumentCaptor<List<AppTableRecord>> recordsCaptor = ArgumentCaptor.forClass(List.class);

        verify(bitableRecordService, times(1)).batchCreateRecords(
                eq(TEST_APP_TOKEN),
                eq(NEW_TABLE_ID),
                recordsCaptor.capture() // 捕获传入的 records 列表
        );

        // 7.3 检查被捕获的 records 列表, 确认日期转换逻辑是否正确
        List<AppTableRecord> capturedRecords = recordsCaptor.getValue();
        assertEquals(1, capturedRecords.size(), "应该只捕获 1 条记录");

        Map<String, Object> fieldsInRecord = capturedRecords.get(0).getFields();
        assertTrue(fieldsInRecord.containsKey("hire_date"), "记录中应包含 hire_date");

        // 这是关键测试: 验证 java.sql.Timestamp 是否被正确转换为了 Long (毫秒)
        Object hireDateValue = fieldsInRecord.get("hire_date");
        assertInstanceOf(Long.class, hireDateValue, "hire_date 应该被转换为 Long 类型(毫秒)");
        assertEquals(946684800000L, (Long) hireDateValue, "时间戳毫秒数不正确");


        // 7.4 验证 jdbcTemplate.update (保存日志) 被调用了 1 次
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT INTO synced_tables"),
                eq(TEST_DB_TABLE_NAME),
                eq(TEST_APP_TOKEN),
                eq(NEW_TABLE_ID),
                eq(TEST_BITABLE_TABLE_NAME)
        );
    }
}