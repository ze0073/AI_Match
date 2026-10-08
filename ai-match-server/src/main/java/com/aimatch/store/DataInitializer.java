package com.aimatch.store;

import com.aimatch.model.entity.Skill;
import com.aimatch.model.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@DependsOn("dataStores")
@RequiredArgsConstructor
public class DataInitializer {

    private final DataStores stores;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (stores.userStore.count() == 0) {
            User admin = new User();
            admin.setUsername("ADMIN");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("System Admin");
            admin.setUserType("ADMIN");
            admin.setStatus(1);
            admin.setPhone("13800000000");
            admin.setEmail("admin@aimatch.com");
            stores.userStore.save(admin);
            System.out.println("[DataInitializer] Created default admin user (username: admin, password: admin123)");
        }

        // Initialize default skills
        if (stores.skillStore.count() == 0) {
            String[][] skills = {
                {"Java", "programming", "1", "Java编程语言"},
                {"Python", "programming", "1", "Python编程语言"},
                {"JavaScript", "programming", "1", "JavaScript编程语言"},
                {"Go", "programming", "1", "Go编程语言"},
                {"C++", "programming", "1", "C++编程语言"},
                {"Spring Boot", "framework", "1", "Spring Boot框架"},
                {"Vue.js", "framework", "1", "Vue.js前端框架"},
                {"React", "framework", "1", "React前端框架"},
                {"MyBatis", "framework", "1", "MyBatis ORM框架"},
                {"Django", "framework", "1", "Django Web框架"},
                {"MySQL", "database", "1", "MySQL关系型数据库"},
                {"Redis", "database", "1", "Redis缓存数据库"},
                {"MongoDB", "database", "1", "MongoDB文档数据库"},
                {"PostgreSQL", "database", "1", "PostgreSQL关系型数据库"},
                {"Elasticsearch", "database", "1", "Elasticsearch搜索引擎"},
                {"Docker", "devops", "1", "Docker容器化"},
                {"Kubernetes", "devops", "1", "Kubernetes容器编排"},
                {"Git", "devops", "1", "Git版本控制"},
                {"Linux", "devops", "1", "Linux操作系统"},
                {"Jenkins", "devops", "1", "Jenkins CI/CD"},
                {"机器学习", "ai", "1", "机器学习技术"},
                {"深度学习", "ai", "1", "深度学习技术"},
                {"自然语言处理", "ai", "1", "NLP自然语言处理"},
                {"计算机视觉", "ai", "1", "计算机视觉技术"},
            };
            for (String[] s : skills) {
                Skill skill = new Skill();
                skill.setName(s[0]);
                skill.setCategory(s[1]);
                skill.setLevel(Integer.parseInt(s[2]));
                skill.setDescription(s[3]);
                skill.setSkillType("TECH");
                skill.setStatus(1);
                stores.skillStore.save(skill);
            }
            System.out.println("[DataInitializer] Created " + skills.length + " default skills");
        }
    }
}
