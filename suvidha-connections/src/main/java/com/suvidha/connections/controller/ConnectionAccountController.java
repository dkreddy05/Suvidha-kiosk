package com.suvidha.connections.controller;

import com.suvidha.connections.dto.LinkAccountRequest;
import com.suvidha.connections.dto.UtilityAccountDTO;
import com.suvidha.connections.dto.AccountLinkResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({ "/api/connections", "/api/v1/connections" })
public class ConnectionAccountController {

    private final RestTemplate restTemplate;
    private final String billingServiceUrl;

    public ConnectionAccountController(
            @Value("${billing.service.url:http://suvidha-billing:8082}") String billingServiceUrl) {
        this.billingServiceUrl = billingServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    @PostMapping("/accounts/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountLinkResponse> linkAccount(
            @Valid @RequestBody LinkAccountRequest req,
            HttpServletRequest request) {

        // Token Propagation: extract incoming Authorization header and forward it
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LinkAccountRequest> entity = new HttpEntity<>(req, headers);
        String targetUrl = billingServiceUrl + "/api/billing/accounts/link";

        try {
            ResponseEntity<UtilityAccountDTO> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    entity,
                    UtilityAccountDTO.class);
            
            UtilityAccountDTO dto = response.getBody();
            AccountLinkResponse res = null;
            if (dto != null) {
                res = AccountLinkResponse.builder()
                        .id(dto.getId())
                        .citizenId(dto.getCitizenId())
                        .accountNumber(dto.getAccountNumber())
                        .utilityType(dto.getUtilityType())
                        .providerName(dto.getProviderName())
                        .address(dto.getAddress())
                        .status("ACTIVE")
                        .build();
            }
            return ResponseEntity.status(response.getStatusCode()).body(res);
        } catch (HttpClientErrorException e) {
            // Forward HTTP client errors (e.g. 400 Bad Request, 403 Forbidden) from Billing Service
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            // Forward HTTP server errors (e.g. 500 Internal Server Error) from Billing Service
            throw new ResponseStatusException(e.getStatusCode(), "Billing service error: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect to billing service", e);
        }
    }
}
