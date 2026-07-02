package com.suvidha.billing.service;

import com.suvidha.billing.enums.ServiceType;

public class OtpContext {
    private String consumerNo;
    private String citizenId;
    private ServiceType serviceType;

    public OtpContext() {
    }

    public OtpContext(String consumerNo, String citizenId, ServiceType serviceType) {
        this.consumerNo = consumerNo;
        this.citizenId = citizenId;
        this.serviceType = serviceType;
    }

    public String getConsumerNo() {
        return consumerNo;
    }

    public void setConsumerNo(String consumerNo) {
        this.consumerNo = consumerNo;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
