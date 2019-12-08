package copy.base.fetcher.config;

import copy.base.fetcher.domain.Client;
import copy.base.fetcher.domain.ClientRowMapper;
import copy.base.fetcher.domain.ClientUpperCaseProcessor;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;

@Configuration
@AllArgsConstructor
public class BatchConfiguration {
    public static final int CHUNK_SIZE = 2048;
    public static final int CORE_POOL_SIZE = 4;
    public static final int MAX_CORE_POOL_SIZE = 16;

    public final JobBuilderFactory jobBuilderFactory;
    public final StepBuilderFactory stepBuilderFactory;
    public final DataSource dataSource;
    public final KafkaTemplate<Long, Client> kafkaTemplate;

    @Bean
    public JdbcCursorItemReader<Client> cursorItemReader() {
        return new JdbcCursorItemReaderBuilder<Client>()
                .dataSource(this.dataSource)
                .name("clientReader")
                .sql("SELECT * FROM CLIENT")
                .rowMapper(new ClientRowMapper())
                .build();
    }

    @Bean
    public ClientUpperCaseProcessor upperCaseProcessor() {
        return new ClientUpperCaseProcessor();
    }

    @Bean
    public KafkaItemWriter<Long, Client> kafkaItemWriter() {
        return new KafkaItemWriterBuilder<Long, Client>()
                .kafkaTemplate(this.kafkaTemplate)
                .itemKeyMapper(Client::getId)
                .build();
    }

    @Bean
    public Job importClientJob(Step step1) {
        return jobBuilderFactory.get("importClientJob")
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(CORE_POOL_SIZE);
        taskExecutor.setMaxPoolSize(MAX_CORE_POOL_SIZE);
        taskExecutor.afterPropertiesSet();

        return stepBuilderFactory.get("step1")
                .<Client, Client>chunk(CHUNK_SIZE)
                .reader(cursorItemReader())
                .processor(upperCaseProcessor())
                .writer(kafkaItemWriter())
                .taskExecutor(taskExecutor)
                .build();
    }
}
