package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.ConnectionStatus;
import com.suvidha.billing.enums.ServiceType;

public class ConnectionStatusResponse {
    private String refNo;
    private ConnectionStatus status;
    private ServiceType serviceType;

    public ConnectionStatusResponse() {
    }

    public ConnectionStatusResponse(String refNo, ConnectionStatus status, ServiceType serviceType) {
        this.refNo = refNo;
        this.status = status;
        this.serviceType = serviceType;
    }

    public static ConnectionStatusResponse of(String refNo, ConnectionStatus status, ServiceType serviceType) {
        return new ConnectionStatusResponse(refNo, status, serviceType);
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
