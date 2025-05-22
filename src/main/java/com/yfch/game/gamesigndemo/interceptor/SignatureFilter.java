package com.yfch.game.gamesigndemo.interceptor;

import com.yfch.game.gamesigndemo.configuration.MerchantConfigProperties;
import com.yfch.game.gamesigndemo.ex.AgGameCallbackException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 签名信息校验过滤器
 * 该过滤器会对所有以 /callback/agGame 开头的请求进行签名信息校验
 * 校验通过后，会将请求体中的内容缓存到 {@link CachedBodyHttpServletRequest} 中，以便后续使用
 * 该过滤器会在 {@link SignatureFilter#doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)} 方法中进行签名信息校验
 *
 * @author Qnxy
 */
@Slf4j
@Component
public class SignatureFilter extends OncePerRequestFilter {

    public static final String CALLBACK_URL = "/callback/agGame";

    private static Mac MAC;
    private final MerchantConfigProperties merchantConfigProperties;


    @SneakyThrows
    public SignatureFilter(MerchantConfigProperties merchantConfigProperties) {
        this.merchantConfigProperties = merchantConfigProperties;

        MAC = Mac.getInstance("HmacSHA256");
        MAC.init(new SecretKeySpec(merchantConfigProperties.getMerchantSecret().getBytes(), "HmacSHA256"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        val requestURI = request.getRequestURI();
        if (!requestURI.startsWith(CALLBACK_URL)) {
            filterChain.doFilter(request, response);
            return;
        }


        val merchantCode = getHeaderValueNotBlank(request, "x-merchant-code");
        if (!merchantCode.equals(this.merchantConfigProperties.getMerchantCode())) {
            throw new AgGameCallbackException("merchant code is invalid: " + merchantCode);
        }

        val sign = getHeaderValueNotBlank(request, "x-sign");
        val timestamp = getHeaderValueNotBlank(request, "x-timestamp");
        val nonce = getHeaderValueNotBlank(request, "x-nonce");
        val contentProcessingType = getHeaderValueNotBlank(request, "x-content-processing-type");

        val cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(request);
        val jsonData = cachedBodyHttpServletRequest.getContentAsString();

        verifySign(merchantCode, sign, timestamp, nonce, contentProcessingType, jsonData);

        filterChain.doFilter(cachedBodyHttpServletRequest, response);
    }

    /**
     * 获取请求头的值，如果为空或空白，则抛出异常
     */
    private String getHeaderValueNotBlank(HttpServletRequest request, String headerName) {
        val h = request.getHeader(headerName);
        if (h == null || h.isBlank()) {
            throw new AgGameCallbackException("header " + headerName + " is required");
        }
        return h;
    }

    /**
     * 校验签名信息
     */
    private void verifySign(String merchantCode, String sign, String timestamp, String nonce, String contentProcessingType, String jsonData) {
        val d = merchantCode + timestamp + nonce + contentProcessingType + jsonData;
        val bytes = MAC.doFinal(d.getBytes(StandardCharsets.UTF_8));

        val s = HexFormat.of().formatHex(bytes);
        if (!Objects.equals(s, sign)) {
            throw new AgGameCallbackException("sign is invalid: " + s);
        }

        log.info("sign verify success");
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() {
            val byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        public String getContentAsString() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }
    }

}