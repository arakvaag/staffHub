package no.decisive.staffhub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StaffHubApplication

fun main(args: Array<String>) {
    runApplication<StaffHubApplication>(*args)
}
