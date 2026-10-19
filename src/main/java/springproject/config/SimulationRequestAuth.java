package springproject.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** 생성기 내부 REST 호출만 식별한다. 인증값은 서버 메모리에만 존재한다. */
@Component
public class SimulationRequestAuth {
    private static final String HEADER = "X-Simulation-Token";
    private final byte[] token;

    public SimulationRequestAuth() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        token = HexFormat.of().formatHex(random).getBytes(StandardCharsets.UTF_8);
    }

    public void addHeader(HttpHeaders headers) {
        headers.set(HEADER, new String(token, StandardCharsets.UTF_8));
    }

    public boolean authenticate(HttpServletRequest request) {
        String supplied = request.getHeader(HEADER);
        return supplied != null && MessageDigest.isEqual(token, supplied.getBytes(StandardCharsets.UTF_8));
    }
}
