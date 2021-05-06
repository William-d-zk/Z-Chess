/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.arena.gateway.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isahl.chess.arena.gateway.service.RookCacheService;

@RestController
@RequestMapping("/rest/calculate")
public class RookCacheApi
{

    private final RookCacheService _RookCacheService;

    @Autowired
    public RookCacheApi(RookCacheService _RookCacheService)
    {
        this._RookCacheService = _RookCacheService;
    }

    @GetMapping(path = "/areaOfCircle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Double> areaOfCircle(@RequestParam int radius)
    {
        double result = _RookCacheService.areaOfCircle(radius);

        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/cache0", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Double> cache0(@RequestParam int radius)
    {
        double result = _RookCacheService.cache0(radius);

        return ResponseEntity.ok(result);
    }
}
