package com.yfch.game.gamesigndemo.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Qnxy
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "merchant")
public class MerchantConfigProperties {

    public String merchantCode;
    public String merchantSecret;

}
