package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.ServiceType;

import java.util.List;

public class ProviderListResponse {
    private ServiceType serviceType;
    private List<String> providers;

    public ProviderListResponse() {
    }

    public ProviderListResponse(ServiceType serviceType, List<String> providers) {
        this.serviceType = serviceType;
        this.providers = providers;
    }

    public static ProviderListResponse of(ServiceType serviceType, List<String> providers) {
        return new ProviderListResponse(serviceType, providers);
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers;
    }
}
