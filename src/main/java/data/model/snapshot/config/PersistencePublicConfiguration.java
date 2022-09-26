/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package data.model.snapshot.config;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "data.model.snapshot.repository",
    entityManagerFactoryRef = "publicEntityManager",
    transactionManagerRef = "publicTransactionManager")
@Profile("!test")
public class PersistencePublicConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "spring.public-datasource")
  public DataSource publicDataSource() {
    return DataSourceBuilder.create().build();
  }


  @Bean
  public JpaVendorAdapter hibernateJpaVendorAdapter() {
    return new HibernateJpaVendorAdapter();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean publicEntityManager(DataSource publicDataSource,
      JpaVendorAdapter hibernateJpaVendorAdapter,
      @Value("${hibernate.dialect}") String hibernateDialect) {
    final var em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(publicDataSource);
    em.setPackagesToScan("data.model.snapshot.model");
    em.setJpaVendorAdapter(hibernateJpaVendorAdapter);

    final var properties = new HashMap<String, Object>();
    properties.put("hibernate.dialect", hibernateDialect);
    em.setJpaPropertyMap(properties);
    return em;
  }

  @Bean
  public PlatformTransactionManager publicTransactionManager(
      AbstractEntityManagerFactoryBean publicEntityManager) {
    final var transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(publicEntityManager.getObject());
    return transactionManager;
  }
}
