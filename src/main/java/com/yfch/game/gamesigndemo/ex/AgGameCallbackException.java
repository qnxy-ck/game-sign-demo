package com.yfch.game.gamesigndemo.ex;

import lombok.NoArgsConstructor;

/**
 * 可在全局异常处理器中捕获该异常，进行统一处理
 * 并根据该异常返回 AG 可接收的 json 信息
 *
 * @author Qnxy
 */
@NoArgsConstructor
public class AgGameCallbackException extends RuntimeException {

    public AgGameCallbackException(String message) {
        super(message);
    }

    public AgGameCallbackException(String message, Throwable cause) {
        super(message, cause);
    }

}
