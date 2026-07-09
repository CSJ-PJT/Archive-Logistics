package com.csj.archive.logistics.batch;

import com.csj.archive.logistics.outbox.OutboxProperties;
import com.csj.archive.logistics.outbox.OutboxPublisher;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OutboxPublishJobConfig {
    @Bean
    public Job outboxPublishJob(JobRepository jobRepository, Step outboxPublishStep) {
        return new JobBuilder("outboxPublishJob", jobRepository)
                .start(outboxPublishStep)
                .build();
    }

    @Bean
    public Step outboxPublishStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  OutboxPublisher outboxPublisher,
                                  OutboxProperties outboxProperties) {
        return new StepBuilder("outboxPublishStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    outboxPublisher.publishAvailable("batch", outboxProperties.getChunkSize());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
