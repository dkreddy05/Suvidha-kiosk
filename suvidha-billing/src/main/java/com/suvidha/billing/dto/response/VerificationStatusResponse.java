package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.VerificationStatus;
import com.suvidha.billing.enums.ServiceType;

public class VerificationStatusResponse {
    private String refNo;
    private VerificationStatus status;
    private String consumerNo;
    private ServiceType serviceType;

    public VerificationStatusResponse() {
    }

    public VerificationStatusResponse(String refNo, VerificationStatus status, String consumerNo,
            ServiceType serviceType) {
        this.refNo = refNo;
        this.status = status;
        this.consumerNo = consumerNo;
        this.serviceType = serviceType;
    }

    public static VerificationStatusResponse of(String refNo, VerificationStatus status, String consumerNo,
            ServiceType serviceType) {
        return new VerificationStatusResponse(refNo, status, consumerNo, serviceType);
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getConsumerNo() {
        return consumerNo;
    }

    public void setConsumerNo(String consumerNo) {
        this.consumerNo = consumerNo;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
