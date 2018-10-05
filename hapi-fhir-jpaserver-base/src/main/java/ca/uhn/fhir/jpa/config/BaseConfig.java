package ca.uhn.fhir.jpa.config;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2018 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.provider.SubscriptionRetriggeringProvider;
import ca.uhn.fhir.jpa.sched.ISchedulerService;
import ca.uhn.fhir.jpa.sched.SchedulerService;
import ca.uhn.fhir.jpa.search.*;
import ca.uhn.fhir.jpa.sp.ISearchParamPresenceSvc;
import ca.uhn.fhir.jpa.sp.SearchParamPresenceSvcImpl;
import ca.uhn.fhir.jpa.subscription.email.SubscriptionEmailInterceptor;
import ca.uhn.fhir.jpa.subscription.resthook.SubscriptionRestHookInterceptor;
import ca.uhn.fhir.jpa.subscription.websocket.SubscriptionWebsocketInterceptor;
import ca.uhn.fhir.jpa.util.IReindexController;
import ca.uhn.fhir.jpa.util.ReindexController;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.Properties;

@Configuration
@EnableJpaRepositories(basePackages = "ca.uhn.fhir.jpa.dao.data")
public abstract class BaseConfig {

	@Autowired
	protected Environment myEnv;

	@Bean
	public DaoRegistry daoRegistry() {
		return new DaoRegistry();
	}


	@Bean(autowire = Autowire.BY_TYPE)
	public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
		return new DatabaseBackedPagingProvider();
	}

	/**
	 * This method should be overridden to provide an actual completed
	 * bean, but it provides a partially completed entity manager
	 * factory with HAPI FHIR customizations
	 */
	protected LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
		configureEntityManagerFactory(retVal, fhirContext());
		return retVal;
	}

	public abstract FhirContext fhirContext();

	@Bean
	public HibernateExceptionTranslator hibernateExceptionTranslator() {
		return new HibernateExceptionTranslator();
	}

	@Bean
	public HibernateJpaDialect hibernateJpaDialectInstance() {
		return new HibernateJpaDialect();
	}

	@Bean
	public IReindexController reindexController() {
		return new ReindexController();
	}

	@Bean
	@Lazy
	public SubscriptionRetriggeringProvider mySubscriptionRetriggeringProvider() {
		return new SubscriptionRetriggeringProvider();
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public ISearchCoordinatorSvc searchCoordinatorSvc() {
		return new SearchCoordinatorSvcImpl();
	}

	@Bean
	public ISearchParamPresenceSvc searchParamPresenceSvc() {
		return new SearchParamPresenceSvcImpl();
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public IStaleSearchDeletingSvc staleSearchDeletingSvc() {
		return new StaleSearchDeletingSvcImpl();
	}

	/**
	 * Note: If you're going to use this, you need to provide a bean
	 * of type {@link ca.uhn.fhir.jpa.subscription.email.IEmailSender}
	 * in your own Spring config
	 */
	@Bean
	@Lazy
	public SubscriptionEmailInterceptor subscriptionEmailInterceptor() {
		return new SubscriptionEmailInterceptor();
	}

	@Bean
	@Lazy
	public SubscriptionRestHookInterceptor subscriptionRestHookInterceptor() {
		return new SubscriptionRestHookInterceptor();
	}

	@Bean
	@Lazy
	public SubscriptionWebsocketInterceptor subscriptionWebsocketInterceptor() {
		return new SubscriptionWebsocketInterceptor();
	}


	public static void configureEntityManagerFactory(LocalContainerEntityManagerFactoryBean theFactory, FhirContext theCtx) {
		theFactory.setJpaDialect(hibernateJpaDialect(theCtx.getLocalizer()));
		theFactory.setPackagesToScan("ca.uhn.fhir.jpa.entity");
		theFactory.setPersistenceProvider(new HibernatePersistenceProvider());
	}

	private static HibernateJpaDialect hibernateJpaDialect(HapiLocalizer theLocalizer) {
		return new HapiFhirHibernateJpaDialect(theLocalizer);
	}

	/**
	 * This lets the "@Value" fields reference properties from the properties file
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ISchedulerService schedulerService() {
		return new SchedulerService();
	}


}
