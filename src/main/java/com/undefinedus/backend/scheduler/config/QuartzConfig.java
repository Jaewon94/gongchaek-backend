package com.undefinedus.backend.scheduler.config;

import com.undefinedus.backend.domain.enums.DiscussionStatus;
import com.undefinedus.backend.scheduler.entity.QuartzJobDetail;
import com.undefinedus.backend.scheduler.entity.QuartzTrigger;
import com.undefinedus.backend.scheduler.job.Analyzing;
import com.undefinedus.backend.scheduler.job.Completed;
import com.undefinedus.backend.scheduler.job.InProgress;
import com.undefinedus.backend.scheduler.job.Scheduled;
import com.undefinedus.backend.scheduler.repository.QuartzJobDetailRepository;
import com.undefinedus.backend.scheduler.repository.QuartzTriggerRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;
import javax.sql.DataSource;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    @Autowired
    private QuartzJobDetailRepository quartzJobDetailRepository;

    @Autowired
    private QuartzTriggerRepository quartzTriggerRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Scheduler scheduler;

    public void scheduleJobs(LocalDateTime targetTime, Long discussionId) throws Exception {
        System.out.println("시간 : " + targetTime.toString());

        // PROPOSED -> SCHEDULED (시작 3시간 전)
//        LocalDateTime startDateTime = targetTime.minusHours(3);
        LocalDateTime startDateTime = targetTime.minusMinutes(2);
        Date startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());

        JobDetail scheduleDetail = JobBuilder.newJob(Scheduled.class)
            .withIdentity("SCHEDULED_" + discussionId.toString())
            .usingJobData("discussionId", discussionId.toString()).build();

        Trigger scheduleTrigger = TriggerBuilder.newTrigger()
            .startAt(startDate)
            .build();

        scheduler.scheduleJob(scheduleDetail, scheduleTrigger);
        saveQuartzJobDetail(discussionId, DiscussionStatus.SCHEDULED);
        saveQuartzTrigger(discussionId, startDate, DiscussionStatus.SCHEDULED);

        // SCHEDULED -> IN_PROGRESS (시작 시간)
        startDate = Date.from(targetTime.atZone(ZoneId.systemDefault()).toInstant());

        JobDetail inProgressDetail = JobBuilder.newJob(InProgress.class)
            .withIdentity("IN_PROGRESS_" + discussionId.toString())
            .usingJobData("discussionId", discussionId.toString()).build();

        Trigger inProgressTrigger = TriggerBuilder.newTrigger()
            .startAt(startDate)
            .build();

        scheduler.scheduleJob(inProgressDetail, inProgressTrigger);
        saveQuartzJobDetail(discussionId, DiscussionStatus.IN_PROGRESS);
        saveQuartzTrigger(discussionId, startDate, DiscussionStatus.IN_PROGRESS);

        // IN_PROGRESS -> ANALYZING (시작 24시간 후)
//        startDateTime = targetTime.plusHours(24);
        startDateTime = targetTime.plusSeconds(30);
        startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());

        JobDetail analyzingDetail = JobBuilder.newJob(Analyzing.class)
            .withIdentity("ANALYZING_" + discussionId.toString())
            .usingJobData("discussionId", discussionId.toString()).build();

        Trigger analyzingTrigger = TriggerBuilder.newTrigger()
            .startAt(startDate)
            .build();

        scheduler.scheduleJob(analyzingDetail, analyzingTrigger);
        saveQuartzJobDetail(discussionId, DiscussionStatus.ANALYZING);
        saveQuartzTrigger(discussionId, startDate, DiscussionStatus.ANALYZING);

        // ANALYZING -> COMPLETED (시작 25시간 후, 분석에 1시간 가정)
//        startDateTime = targetTime.plusHours(25);
        startDateTime = targetTime.plusSeconds(30);
        startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());

        JobDetail completedDetail = JobBuilder.newJob(Completed.class)
            .withIdentity("COMPLETED_" + discussionId.toString())
            .usingJobData("discussionId", discussionId.toString()).build();

        Trigger completedTrigger = TriggerBuilder.newTrigger()
            .startAt(startDate)
            .build();

        scheduler.scheduleJob(completedDetail, completedTrigger);
        saveQuartzJobDetail(discussionId, DiscussionStatus.COMPLETED);
        saveQuartzTrigger(discussionId, startDate, DiscussionStatus.COMPLETED);
    }

    private void saveQuartzJobDetail(Long discussionId, DiscussionStatus newStatus) {
        QuartzJobDetail quartzJobDetail = new QuartzJobDetail();
        quartzJobDetail.setSchedName("discussion");
        quartzJobDetail.setJobName("discussion_" + discussionId + "_" + newStatus);
        quartzJobDetail.setJobGroup(newStatus.name());
        String jobClassName = getJobClassNameForStatus(newStatus);
        quartzJobDetail.setJobClassName(jobClassName);
        quartzJobDetailRepository.save(quartzJobDetail);
    }

    private void saveQuartzTrigger(Long discussionId, Date executionTime,
        DiscussionStatus newStatus) {
        QuartzTrigger quartzTrigger = new QuartzTrigger();
        quartzTrigger.setTriggerName(
            "trigger_changeStatus_" + discussionId.toString() + "_" + newStatus);
        quartzTrigger.setTriggerGroup(newStatus.name());
        quartzTrigger.setStartTime(executionTime.getTime());
        quartzTrigger.setTriggerType("SIMPLE");
        quartzTrigger.setSchedName("discussion ID : " + discussionId.toString());
        quartzTrigger.setStartTimeEasy(executionTime);

        quartzTriggerRepository.save(quartzTrigger);
    }

    private String getJobClassNameForStatus(DiscussionStatus status) {
        switch (status) {
            case SCHEDULED:
                return Scheduled.class.getName();
            case IN_PROGRESS:
                return InProgress.class.getName();
            case ANALYZING:
                return Analyzing.class.getName();
            case COMPLETED:
                return Completed.class.getName();
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }
}