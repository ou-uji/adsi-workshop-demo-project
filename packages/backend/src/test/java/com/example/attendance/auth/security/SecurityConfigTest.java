package com.example.attendance.auth.security;

import com.example.attendance.common.config.security.EmployeeUserDetails;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private UUID testEmployeeId;
    private UUID testDepartmentId;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        entityManager.flush();

        testDepartmentId = UUID.randomUUID();
        var department = Department.builder()
            .id(testDepartmentId)
            .name("テスト部署")
            .build();
        entityManager.persist(department);
        entityManager.flush();

        testEmployeeId = UUID.randomUUID();
        var employee = Employee.builder()
            .id(testEmployeeId)
            .name("テストユーザー")
            .email(TEST_EMAIL)
            .password(passwordEncoder.encode(TEST_PASSWORD))
            .department(department)
            .role(Role.ADMIN)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();
        employeeRepository.save(employee);
        entityManager.flush();
    }

    @Test
    @DisplayName("未認証で保護エンドポイントにアクセスすると401が返される")
    void protectedEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("正しい認証情報でログインすると200とユーザー情報が返される")
    void login_validCredentials_returns200WithUserInfo() throws Exception {
        String loginJson = "{\"email\": \"%s\", \"password\": \"%s\"}"
            .formatted(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("テストユーザー"))
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("誤ったパスワードでログインすると401が返される")
    void login_wrongPassword_returns401() throws Exception {
        String loginJson = "{\"email\": \"%s\", \"password\": \"wrongpassword\"}"
            .formatted(TEST_EMAIL);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    @DisplayName("認証済みでGET /api/auth/meにアクセスすると200とユーザー情報が返される")
    void getMe_authenticated_returns200() throws Exception {
        MockHttpSession session = createAuthenticatedSession();

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("テストユーザー"))
            .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @DisplayName("ログアウトするとセッションが破棄される")
    void logout_authenticated_invalidatesSession() throws Exception {
        MockHttpSession session = createAuthenticatedSession();

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf().asHeader()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    private MockHttpSession createAuthenticatedSession() {
        var info = new EmployeeUserDetails.EmployeeInfo(
            testEmployeeId, "テストユーザー",
            testDepartmentId, "テスト部署",
            Role.ADMIN, false
        );
        var userDetails = new EmployeeUserDetails(
            TEST_EMAIL,
            passwordEncoder.encode(TEST_PASSWORD),
            true,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            info
        );
        var authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext);

        return session;
    }
}
