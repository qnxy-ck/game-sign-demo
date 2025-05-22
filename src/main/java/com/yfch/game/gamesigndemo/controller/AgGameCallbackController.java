package com.yfch.game.gamesigndemo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.yfch.game.gamesigndemo.interceptor.SignatureFilter.CALLBACK_URL;

/**
 * @author Qnxy
 */
@RestController
@RequestMapping(CALLBACK_URL)
public class AgGameCallbackController {

    // TODO: 2025/5/22 实现回调接口

}
