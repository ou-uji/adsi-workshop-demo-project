package com.example.attendance.common.config.security;

import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "メールアドレスまたはパスワードが正しくありません"));

        var info = new EmployeeUserDetails.EmployeeInfo(
            employee.getId(),
            employee.getName(),
            employee.getDepartment().getId(),
            employee.getDepartment().getName(),
            employee.getRole(),
            employee.isManager()
        );
        return new EmployeeUserDetails(
            employee.getEmail(),
            employee.getPassword(),
            employee.getRetireDate() == null,
            List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())),
            info
        );
    }
}
