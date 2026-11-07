package com.jobboard.api.config;

import com.jobboard.api.entity.Category;
import com.jobboard.api.entity.Skill;
import com.jobboard.api.repository.CategoryRepository;
import com.jobboard.api.repository.SkillRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(CategoryRepository categoryRepository, SkillRepository skillRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(List.of(
                        category("Yazılım", "yazilim"),
                        category("Tasarım", "tasarim"),
                        category("Pazarlama", "pazarlama"),
                        category("Satış", "satis"),
                        category("Finans", "finans"),
                        category("İnsan Kaynakları", "insan-kaynaklari")
                ));
            }
            if (skillRepository.count() == 0) {
                skillRepository.saveAll(List.of(
                        skill("Java", "java"),
                        skill("Spring Boot", "spring-boot"),
                        skill("SQL", "sql"),
                        skill("PostgreSQL", "postgresql"),
                        skill("Docker", "docker"),
                        skill("React", "react"),
                        skill("JavaScript", "javascript"),
                        skill("TypeScript", "typescript"),
                        skill("Python", "python"),
                        skill("AWS", "aws"),
                        skill("Git", "git"),
                        skill("REST API", "rest-api"),
                        skill("İletişim", "iletisim"),
                        skill("Liderlik", "liderlik")
                ));
            }
        };
    }

    private Category category(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return category;
    }

    private Skill skill(String name, String slug) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setSlug(slug);
        return skill;
    }
}
