package com.retail.rec;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZipmartBackendSpringApplication {

	public static void main(String[] args) {
		// pgjdbc sends the JVM's default timezone as a connection startup
		// parameter. On some Windows hosts that resolves to a legacy alias
		// (e.g. "Asia/Saigon") that Postgres 17 doesn't recognize, which
		// fails the connection before any SQL runs. Force UTC so this is
		// consistent regardless of the host's OS timezone naming.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ZipmartBackendSpringApplication.class, args);
	}

}
