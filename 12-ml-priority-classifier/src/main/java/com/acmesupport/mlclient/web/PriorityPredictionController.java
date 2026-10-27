package com.acmesupport.mlclient.web;

import com.acmesupport.mlclient.port.PriorityPredictionPort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriorityPredictionController {

    private final PriorityPredictionPort priorityPredictionPort;

    public PriorityPredictionController(PriorityPredictionPort priorityPredictionPort) {
        this.priorityPredictionPort = priorityPredictionPort;
    }

    public record PredictHttpRequest(
            @NotBlank String subject,
            @NotBlank String body,
            @NotBlank String planTier,
            @Min(0) int accountAgeDays,
            @Min(0) int priorTicketCount30d) {
    }

    /**
     * POST /api/tickets/predict-priority
     * {"subject": "...", "body": "...", "planTier": "BUSINESS", "accountAgeDays": 400, "priorTicketCount30d": 3}
     *
     * Same shape as every other AI-backed endpoint across this series: a plain
     * controller depending on a port interface, unaware that this particular port
     * happens to be backed by a trained model instead of a prompt.
     */
    @PostMapping("/api/tickets/predict-priority")
    public PriorityPredictionPort.PriorityPrediction predict(@RequestBody PredictHttpRequest request) {
        return priorityPredictionPort.predict(
                request.subject(), request.body(), request.planTier(),
                request.accountAgeDays(), request.priorTicketCount30d());
    }
}
