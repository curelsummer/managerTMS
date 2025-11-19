package cc.mrbird.febs;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@MapperScan({"cc.mrbird.febs.system.dao", "cc.mrbird.febs.cos.dao"})
public class FebsApplication {

    public static void main(String[] args) {
        System.out.println("[BOOT] Starting FebsApplication ...");
        try {
            new SpringApplicationBuilder(FebsApplication.class).run(args);
        } catch (Throwable t) {
            System.err.println("[BOOT] Application failed to start:");
            t.printStackTrace();
            // 确保非零退出码
            System.exit(1);
        }
    }
}
