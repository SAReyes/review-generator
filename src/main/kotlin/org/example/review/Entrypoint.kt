package org.example.review

import org.example.review.Entrypoint.log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.Address
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.QueueSpecification
import reactor.rabbitmq.ReactorRabbitMq
import reactor.rabbitmq.SenderOptions
import java.time.Duration
import java.util.*
import kotlin.math.round

object Entrypoint {
    val log = LoggerFactory.getLogger(Entrypoint::class.java)!!
    val rng = Random()
    val mapper = jacksonObjectMapper()
    val ids = mapper.readValue<List<String>>(Entrypoint::class.java.getResource("/ids.json"))
    const val queue = "review-queue"
}

fun main(args: Array<String>) {
    var delay = 0L
    if (args.isNotEmpty()) {
        try {
            delay = args[0].toLong()
        } catch (_: NumberFormatException) {
        }
    }

    val sender = ReactorRabbitMq.createSender(
        SenderOptions().connectionSupplier { it.newConnection(listOf(Address("rabbit"))) }
    )

    sender.declareQueue(QueueSpecification.queue(Entrypoint.queue)).block()

    sender
        .sendWithPublishConfirms(
            Flux.generate<Review> { it.next(
                Review(
                    getId(),
                    getRating(),
                    getReview()
                )
            ) }
                .map { OutboundMessage("", Entrypoint.queue, Entrypoint.mapper.writeValueAsBytes(it)) }
                .let { if (delay != 0L) it.delayElements(Duration.ofMillis(delay)) else it }
        )
        .doOnError { log.error("Send failed", it) }
        .subscribe {
            if (it.isAck) log.info("Message ${String(it.outboundMessage.body)} sent successfully")
        }
}

fun getId() = Entrypoint.ids.shuffled().first()
fun getRating() = round(Math.log(Entrypoint.rng.nextInt(150) + 1.0) * 10.0) / 10.0
fun getReview() = "Review: ${UUID.randomUUID()}"

data class Review(val id: String, val rating: Double, val comment: String)