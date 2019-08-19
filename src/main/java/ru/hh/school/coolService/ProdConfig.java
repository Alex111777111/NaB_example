package ru.hh.school.coolService;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.hh.nab.core.CoreProdConfig;
import ru.hh.nab.hibernate.HibernateProdConfig;
import ru.hh.nab.hibernate.MappingConfig;
import ru.hh.school.coolService.dao.EmployeeDao;
import ru.hh.school.coolService.dao.ResumeDao;
import ru.hh.school.coolService.entities.Employee;
import ru.hh.school.coolService.entities.Resume;
import ru.hh.school.coolService.resources.EmployeeResource;

import javax.sql.DataSource;

import ru.hh.school.coolService.resources.RestTestBean;
import ru.hh.school.coolService.services.EmployeeService;
import ru.hh.school.coolService.spring.postProcessor.InjectRestTimeoutBeanPostProcessor;

@Configuration
@Import({
    CoreProdConfig.class,
    HibernateProdConfig.class,
    EmployeeResource.class,

    InjectRestTimeoutBeanPostProcessor.class,

    EmployeeDao.class,
    ResumeDao.class,
    EmployeeService.class
})
public class ProdConfig {

    private final ApplicationContext context;

    public ProdConfig(ApplicationContext context) {
        this.context = context;
    }

    @Bean
    public RestTestBean getTestBean() {
        return new RestTestBean(123);
    }


  @Bean
  MappingConfig mappingConfig() {
    return new MappingConfig(
        Resume.class,
        Employee.class
    );
  }

  @Bean(destroyMethod = "shutdown")
  DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.HSQL)
        .addScript("db/sql/create-db.sql")
        .build();
  }

}
