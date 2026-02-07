package dev.pawin.backend_learning_buddy.quiz.listener;

import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizPreviewJob;
import dev.pawin.backend_learning_buddy.quiz.repository.QuizPreviewJobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuizJobStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(QuizJobStartupListener.class);

    private final QuizPreviewJobRepository jobRepository;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        resetZombieJobs();
    }

    @Transactional
    protected void resetZombieJobs() {
        List<QuizPreviewJob> stuckJobs = jobRepository.findByStatus(JobStatus.PROCESSING);

        if (!stuckJobs.isEmpty()) {
            logger.warn("Found {} zombie quiz jobs from previous server run. Resetting to FAILED.",
                    stuckJobs.size());

            for (QuizPreviewJob job : stuckJobs) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Job interrupted due to server restart");
                jobRepository.save(job);

                logger.info("Reset zombie job {} for user {} to FAILED",
                        job.getJobId(),
                        job.getUser().getUsername());
            }

            logger.info("Successfully reset {} zombie jobs", stuckJobs.size());
        } else {
            logger.info("No zombie quiz jobs found. All clean.");
        }
    }
}
