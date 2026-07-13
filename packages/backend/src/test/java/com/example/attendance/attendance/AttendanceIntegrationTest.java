package com.example.attendance.attendance;

import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession employeeSession;
    private MockHttpSession adminSession;
    private MockHttpSession managerSession;
    private UUID employeeId;
    private UUID managerId;

    @BeforeEach
    void setUp() throws Exception {
        var department = Department.builder()
            .id(UUID.randomUUID())
            .name("開発部")
            .build();
        entityManager.persist(department);

        employeeId = UUID.randomUUID();
        var employee = Employee.builder()
            .id(employeeId)
            .name("一般社員")
            .email("employee@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        entityManager.persist(employee);

        managerId = UUID.randomUUID();
        var manager = Employee.builder()
            .id(managerId)
            .name("部長")
            .email("manager@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(true)
            .hireDate(LocalDate.of(2020, 4, 1))
            .build();
        entityManager.persist(manager);

        var admin = Employee.builder()
            .id(UUID.randomUUID())
            .name("管理者")
            .email("admin@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.ADMIN)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();
        entityManager.persist(admin);
        entityManager.flush();

        employeeSession = login("employee@example.com", "password123");
        managerSession = login("manager@example.com", "password123");
        adminSession = login("admin@example.com", "password123");
    }

    @Test
    @DisplayName("出勤打刻すると201と打刻記録が返される")
    void clockIn_returns201() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.workDate").exists())
            .andExpect(jsonPath("$.clockIn").exists())
            .andExpect(jsonPath("$.clockOut").isEmpty());
    }

    @Test
    @DisplayName("出勤→退勤のフローが正常に動作する")
    void clockInThenClockOut_succeeds() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/attendance/clock-out")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clockOut").exists());
    }

    @Test
    @DisplayName("出勤していない状態で退勤すると409が返される")
    void clockOut_noClockedIn_returns409() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-out")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("出勤→退勤後に再度出勤できる")
    void clockInAgain_afterClockOut_succeeds() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/attendance/clock-out")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("出勤中に再度出勤打刻すると409が返り、退勤打刻は正常に成功する")
    void clockInTwice_thenClockOut_secondClockInReturns409() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());

        // 2回目の出勤打刻は拒否される（二重打刻防止）
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isConflict());

        // 二重打刻が防がれているため退勤打刻は正常に成功する
        mockMvc.perform(post("/api/attendance/clock-out")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clockOut").exists());
    }

    @Test
    @DisplayName("未打刻状態のtodayステータスはNOT_CLOCKED_IN")
    void getTodayStatus_notClockedIn_returnsNotClockedIn() throws Exception {
        mockMvc.perform(get("/api/attendance/today")
                .session(employeeSession)
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOT_CLOCKED_IN"))
            .andExpect(jsonPath("$.records", hasSize(0)));
    }

    @Test
    @DisplayName("出勤後のtodayステータスはCLOCKED_IN")
    void getTodayStatus_afterClockIn_returnsClockedIn() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/attendance/today")
                .session(employeeSession)
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOCKED_IN"))
            .andExpect(jsonPath("$.records", hasSize(1)));
    }

    @Test
    @DisplayName("退勤後のtodayステータスはCLOCKED_OUT")
    void getTodayStatus_afterClockOut_returnsClockedOut() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/attendance/clock-out")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/attendance/today")
                .session(employeeSession)
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOCKED_OUT"))
            .andExpect(jsonPath("$.records", hasSize(1)));
    }

    @Test
    @DisplayName("月次勤怠履歴を取得できる")
    void getHistory_returnsMonthlyHistory() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/attendance/clock-out")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isOk());

        var currentMonth = YearMonth.now().toString();

        mockMvc.perform(get("/api/attendance/history")
                .session(employeeSession)
                .param("employeeId", employeeId.toString())
                .param("month", currentMonth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.month").value(currentMonth))
            .andExpect(jsonPath("$.days").isArray())
            .andExpect(jsonPath("$.summary").exists())
            .andExpect(jsonPath("$.summary.workDays").value(1));
    }

    @Test
    @DisplayName("メモ付き出勤打刻→履歴取得でメモが返される")
    void clockInWithMemo_thenHistory_returnsMemo() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString())
                .contentType(APPLICATION_JSON)
                .content("{\"memo\":\"電車遅延のため遅刻\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.clockInMemo").value("電車遅延のため遅刻"));

        var currentMonth = YearMonth.now().toString();
        mockMvc.perform(get("/api/attendance/history")
                .session(employeeSession)
                .param("employeeId", employeeId.toString())
                .param("month", currentMonth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days[0].records[0].clockInMemo").value("電車遅延のため遅刻"));
    }

    @Test
    @DisplayName("メモ編集→再取得で更新が反映される（承認不要・即時）")
    void updateMemo_thenRefetch_reflectsChange() throws Exception {
        var createResult = mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString())
                .contentType(APPLICATION_JSON)
                .content("{\"memo\":\"初回メモ\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        var body = createResult.getResponse().getContentAsString();
        var recordId = com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();
        var version = com.jayway.jsonpath.JsonPath.read(body, "$.version");

        mockMvc.perform(patch("/api/attendance/records/{id}/memo", recordId)
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString())
                .contentType(APPLICATION_JSON)
                .content("{\"clockInMemo\":\"修正後メモ\",\"clockOutMemo\":null,\"version\":" + version + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clockInMemo").value("修正後メモ"));
    }

    @Test
    @DisplayName("他人のレコードのメモを編集しようとすると403が返される")
    void updateMemo_notOwner_returns403() throws Exception {
        var createResult = mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isCreated())
            .andReturn();

        var body = createResult.getResponse().getContentAsString();
        var recordId = com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();
        var version = com.jayway.jsonpath.JsonPath.read(body, "$.version");

        // manager が employee のレコードを編集 → 403
        mockMvc.perform(patch("/api/attendance/records/{id}/memo", recordId)
                .session(managerSession)
                .with(csrf())
                .param("employeeId", managerId.toString())
                .contentType(APPLICATION_JSON)
                .content("{\"clockInMemo\":\"改ざん\",\"version\":" + version + "}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("201文字メモの出勤打刻は400が返される")
    void clockIn_memoOver200_returns400() throws Exception {
        var longMemo = "あ".repeat(201);

        mockMvc.perform(post("/api/attendance/clock-in")
                .session(employeeSession)
                .with(csrf())
                .param("employeeId", employeeId.toString())
                .contentType(APPLICATION_JSON)
                .content("{\"memo\":\"" + longMemo + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("マネージャーがチーム勤怠を取得できる")
    void getTeamAttendance_asManager_returnsTeamData() throws Exception {
        var currentMonth = YearMonth.now().toString();

        mockMvc.perform(get("/api/attendance/team")
                .session(managerSession)
                .param("managerId", managerId.toString())
                .param("month", currentMonth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("ADMIN: 全社員勤怠を取得できる")
    void getAllAttendance_asAdmin_returnsAllData() throws Exception {
        var currentMonth = YearMonth.now().toString();

        mockMvc.perform(get("/api/attendance/all")
                .session(adminSession)
                .param("month", currentMonth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("EMPLOYEE: 全社員勤怠（/all）にアクセスすると403が返される")
    void getAllAttendance_asEmployee_returns403() throws Exception {
        var currentMonth = YearMonth.now().toString();

        mockMvc.perform(get("/api/attendance/all")
                .session(employeeSession)
                .param("month", currentMonth))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未認証で出勤打刻すると401が返される")
    void clockIn_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                .with(csrf())
                .param("employeeId", employeeId.toString()))
            .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) Objects.requireNonNull(result.getRequest().getSession());
    }
}
