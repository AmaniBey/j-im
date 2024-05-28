package org.dromara;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动程序
 *
 * @author Lion Li
 */

@SpringBootApplication
@EnableScheduling
public class DromaraApplication implements CommandLineRunner {

    @Value("${spring.profiles.active}")
    public String active;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DromaraApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ 启动成功   ლ(´ڡ`ლ)ﾞ");

    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("激活的配置：" + active);
//        Collection<String> loginKey = RedisUtils.keys("Authorization:login:*");
//        for (String key : loginKey) {
//            RedisUtils.deleteObject(key);
//        }
//        Collection<String> onlineKey = RedisUtils.keys("online_tokens:*");
//        for (String s : onlineKey) {
//            RedisUtils.deleteObject(s);
//        }
    }
}