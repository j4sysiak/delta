package com.delta.bank

import java.util.concurrent.Executors
import spock.lang.Specification

class VirtualThreadsSpec extends Specification {

    def "creates and runs virtual threads"() {
        given:
        def results = Collections.synchronizedList(new ArrayList<String>())

        when:
        def executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            10.times { i ->
                executor.submit {
                    results << "task-$i"
                }
            }
        } finally {
            executor.close()
        }

        then:
        results.size() == 10
        results.every { it.startsWith('task-') }
    }
}
