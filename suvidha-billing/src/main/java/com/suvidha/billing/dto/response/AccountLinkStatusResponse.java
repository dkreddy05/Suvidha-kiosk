package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.LinkRequestStatus;
import com.suvidha.billing.enums.ServiceType;

public class AccountLinkStatusResponse {
    private LinkRequestStatus status;
    private String consumerNo;
    private ServiceType serviceType;

    public AccountLinkStatusResponse() {
    }

    public AccountLinkStatusResponse(LinkRequestStatus status, String consumerNo, ServiceType serviceType) {
        this.status = status;
        this.consumerNo = consumerNo;
        this.serviceType = serviceType;
    }

    public static AccountLinkStatusResponse of(LinkRequestStatus status, String consumerNo, ServiceType serviceType) {
        return new AccountLinkStatusResponse(status, consumerNo, serviceType);
    }

    public LinkRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LinkRequestStatus status) {
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
