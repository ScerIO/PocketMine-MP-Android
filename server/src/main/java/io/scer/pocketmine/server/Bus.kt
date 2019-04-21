package io.scer.pocketmine.server

import android.text.SpannableStringBuilder
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.BehaviorSubject
import io.scer.pocketmine.server.utils.fromHtml
import io.scer.pocketmine.server.utils.toHTML

class StartEvent
class StopEvent
data class ErrorEvent(val message: String?,
                      val type: Errors)
data class UpdateStatEvent(val state: Map<String, String>)

object ServerBus {
    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)

    object Log {
        private val log = BehaviorSubject.create<SpannableStringBuilder>()
        private val currentLog = SpannableStringBuilder()

        fun message(text: String) {
            currentLog.append(text.toHTML().fromHtml())
            log.onNext(currentLog)
        }

        fun clear() {
            currentLog.clear()
            log.onNext(currentLog)
        }

        // Listen should return an Observable and not the publisher
        // Using ofType we filter only events that match that class type
        val listen: BehaviorSubject<SpannableStringBuilder> get() = log
    }
}