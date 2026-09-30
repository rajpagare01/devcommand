package com.devcommand.devcommand.jobs.service;

import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Transactional
public class JobApplicationIntegrationTest extends com.devcommand.devcommand.integration.AbstractIntegrationTest {

    @Autowired
    private JobApplicationService service;

    @Autowired
    private JobApplicationRepository jobRepo;

    @Autowired
    private UserRepository userRepo;

    @Test
    public void testFilteringAndSorting() {
        User u = User.builder().name("Test").email("test" + System.currentTimeMillis() + "@a.com").password("pwd").build();
        userRepo.save(u);

        JobApplication j1 = JobApplication.builder()
                .company("Google")
                .role("Java Dev")
                .status(ApplicationStatus.INTERVIEW)
                .applicationDate(LocalDate.now())
                .user(u)
                .build();
        JobApplication j2 = JobApplication.builder()
                .company("Amazon")
                .role("Java Senior")
                .status(ApplicationStatus.APPLIED)
                .applicationDate(LocalDate.now())
                .user(u)
                .build();
        JobApplication j3 = JobApplication.builder()
                .company("Google")
                .role("Python Dev")
                .status(ApplicationStatus.INTERVIEW)
                .applicationDate(LocalDate.now())
                .user(u)
                .build();
                
        jobRepo.saveAll(List.of(j1, j2, j3));

        Page<JobApplicationResponse> page = service.getAll(
                u.getId(),
                "Google",       // company
                null,           // role
                null,           // source
                "INTERVIEW",    // status
                "java",         // search
                0,              // page
                20,             // size
                "company",      // sortBy
                "asc"           // direction
        );

        System.out.println("TOTAL ELEMENTS FOUND: " + page.getTotalElements());
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).role()).isEqualTo("Java Dev");
    }
}
