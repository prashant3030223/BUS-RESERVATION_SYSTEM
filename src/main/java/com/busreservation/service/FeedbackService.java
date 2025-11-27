package com.busreservation.service;

import com.busreservation.model.Feedback;
import com.busreservation.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback saveFeedback(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderBySubmittedAtDesc();
    }

    public List<Feedback> getFeedbackByStatus(String status) {
        return feedbackRepository.findByStatus(status);
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    public void updateFeedbackStatus(Long id, String status) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(id);
        if (feedbackOpt.isPresent()) {
            Feedback feedback = feedbackOpt.get();
            feedback.setStatus(status);
            feedbackRepository.save(feedback);
        }
    }

    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }

    public long getNewFeedbackCount() {
        return feedbackRepository.countByStatus("NEW");
    }

    public long getTotalFeedbackCount() {
        return feedbackRepository.count();
    }
}
