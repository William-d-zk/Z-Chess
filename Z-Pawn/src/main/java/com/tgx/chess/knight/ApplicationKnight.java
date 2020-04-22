package com.tgx.chess.knight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author william.d.zk
 * @date 2020/4/23
 */
@SpringBootApplication(scanBasePackages = { "com.tgx.chess.knight.config",
                                            "com.tgx.chess.knight.raft",
                                            "com.tgx.chess.knight.json",
                                            "com.tgx.chess.knight.cluster" })
public class ApplicationKnight
{
    public static void main(String[] args)
    {
        SpringApplication.run(ApplicationKnight.class, args);
    }
}
