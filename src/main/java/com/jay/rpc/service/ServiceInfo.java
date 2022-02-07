package com.jay.rpc.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * <p>
 *  RPC服务信息
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 20:37
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class ServiceInfo {
    private String serviceName;
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInfo that = (ServiceInfo) o;
        return version == that.version && Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, serviceName);
    }
}
