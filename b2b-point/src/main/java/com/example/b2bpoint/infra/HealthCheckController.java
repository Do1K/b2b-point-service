package com.example.b2bpoint.infra;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

    private final Environment env;


    public HealthCheckController(Environment env) {
        this.env = env;
    }

    /**
     * 서버의 상태와 현재 활성화된 프로파일을 확인하는 API
     * 배포 후 'http://서버IP:8080/health-check'로 호출하여 확인
     */
    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck() {

        Map<String, Object> responseData = new LinkedHashMap<>();

        responseData.put("status", "ok");
        responseData.put("message", "Server is running smoothly!");



        responseData.put("activeProfiles", Arrays.asList(env.getActiveProfiles().length > 0 ? env.getActiveProfiles() : new String[]{"default"}));

        return ResponseEntity.ok(responseData);
    }
}
