package com.jingxuan.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * 只在显式配置的反向代理 CIDR 来源时采纳 Nginx 覆盖写入的单值 X-Real-IP。
 */
@Component
public class TrustedProxyClientIpResolver {

    private static final String REAL_IP_HEADER = "X-Real-IP";
    private final List<Cidr> trustedProxyCidrs;

    public TrustedProxyClientIpResolver(
            @Value("${jingxuan.security.trusted-proxy-cidrs:127.0.0.1/32,::1/128}") String configuredCidrs) {
        this.trustedProxyCidrs = parseCidrs(configuredCidrs);
    }

    public String resolve(HttpServletRequest request) {
        InetAddress remoteAddress = literal(request.getRemoteAddr());
        if (remoteAddress == null) {
            return "unknown";
        }
        String remote = remoteAddress.getHostAddress();
        if (!isTrustedProxy(remoteAddress)) {
            return remote;
        }

        Enumeration<String> values = request.getHeaders(REAL_IP_HEADER);
        if (values == null || !values.hasMoreElements()) {
            return remote;
        }
        String forwarded = values.nextElement();
        if (values.hasMoreElements() || forwarded == null || forwarded.isBlank()
                || forwarded.indexOf(',') >= 0 || forwarded.indexOf('%') >= 0) {
            return remote;
        }
        InetAddress clientAddress = literal(forwarded.trim());
        return clientAddress == null ? remote : clientAddress.getHostAddress();
    }

    private boolean isTrustedProxy(InetAddress address) {
        return trustedProxyCidrs.stream().anyMatch(cidr -> cidr.contains(address));
    }

    private static List<Cidr> parseCidrs(String configuredCidrs) {
        if (configuredCidrs == null || configuredCidrs.isBlank()) {
            throw new IllegalArgumentException("至少需要配置一个可信反向代理 CIDR");
        }
        return Arrays.stream(configuredCidrs.split(",", -1))
                .map(String::trim)
                .map(Cidr::parse)
                .toList();
    }

    private static InetAddress literal(String value) {
        if (value == null || value.isBlank() || value.indexOf('%') >= 0) {
            return null;
        }
        String candidate = value.trim();
        byte[] ipv4 = parseIpv4(candidate);
        try {
            if (ipv4 != null) {
                return InetAddress.getByAddress(ipv4);
            }
            if (candidate.indexOf(':') < 0 || !candidate.matches("[0-9A-Fa-f:.]+")) {
                return null;
            }
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static byte[] parseIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] address = new byte[4];
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty() || part.length() > 3
                    || !part.chars().allMatch(character -> character >= '0' && character <= '9')) {
                return null;
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) {
                return null;
            }
            address[index] = (byte) octet;
        }
        return address;
    }

    private record Cidr(byte[] network, int prefixLength) {

        static Cidr parse(String value) {
            int separator = value == null ? -1 : value.lastIndexOf('/');
            if (separator <= 0 || separator == value.length() - 1 || value.indexOf('/') != separator) {
                throw new IllegalArgumentException("可信反向代理 CIDR 格式无效");
            }
            InetAddress address = literal(value.substring(0, separator));
            if (address == null) {
                throw new IllegalArgumentException("可信反向代理必须是 IP 字面量");
            }
            try {
                int prefix = Integer.parseInt(value.substring(separator + 1));
                int bits = address.getAddress().length * Byte.SIZE;
                if (prefix < 0 || prefix > bits) {
                    throw new IllegalArgumentException("可信反向代理 CIDR 前缀无效");
                }
                return new Cidr(address.getAddress(), prefix);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("可信反向代理 CIDR 前缀无效", exception);
            }
        }

        boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / Byte.SIZE;
            for (int index = 0; index < fullBytes; index++) {
                if (candidate[index] != network[index]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % Byte.SIZE;
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (Byte.SIZE - remainingBits);
            return (Byte.toUnsignedInt(candidate[fullBytes]) & mask)
                    == (Byte.toUnsignedInt(network[fullBytes]) & mask);
        }
    }
}
